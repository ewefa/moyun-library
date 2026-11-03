package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BizException;
import com.library.common.Constants;
import com.library.common.PageResult;
import com.library.common.RedisCache;
import com.library.common.RedisKeys;
import com.library.common.UserContext;
import com.library.config.LibraryProperties;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.UserMapper;
import com.library.vo.BorrowSummaryVO;
import com.library.vo.BorrowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 借阅核心业务：借书、还书、续借、记录查询。
 * 并发安全依赖数据库条件更新（库存 / 归还 / 续借），而不是先查后改。
 */
@Service
@RequiredArgsConstructor
public class BorrowService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BorrowRecordMapper recordMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final HotRankService hotRankService;
    private final RedisCache cache;
    private final LibraryProperties properties;

    @Transactional
    public BorrowVO borrow(Long bookId) {
        Long userId = UserContext.require().getId();
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != Constants.USER_ENABLED) {
            throw BizException.forbidden("账号已被禁用");
        }
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw BizException.notFound("图书不存在");
        }
        if (book.getStatus() == null || book.getStatus() != Constants.BOOK_ON_SHELF) {
            throw BizException.badRequest("该图书已下架，暂不可借阅");
        }
        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw BizException.conflict("该书暂无可借副本");
        }
        if (recordMapper.countOverdueByUser(userId) > 0) {
            throw BizException.badRequest("您有逾期未还的图书，请先归还后再借阅");
        }
        if (recordMapper.countActiveByUser(userId) >= properties.getBorrow().getMaxBorrowing()) {
            throw BizException.badRequest("最多同时借阅 " + properties.getBorrow().getMaxBorrowing()
                    + " 册，请先归还部分图书");
        }
        if (recordMapper.countActiveByBook(userId, bookId) > 0) {
            throw BizException.conflict("您已借阅该书且尚未归还");
        }

        // 关键：条件更新扣库存，影响行数为 0 说明被其他请求抢先借完
        if (bookMapper.decreaseStock(bookId) == 0) {
            throw BizException.conflict("手慢了，该书已被借完");
        }

        LocalDateTime now = LocalDateTime.now();
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowTime(now);
        record.setDueTime(now.plusDays(properties.getBorrow().getLoanDays()));
        record.setRenewCount(0);
        record.setStatus(Constants.BORROW_ACTIVE);
        recordMapper.insert(record);

        afterCommit(() -> {
            hotRankService.recordBorrow(bookId);
            cache.delete(RedisKeys.HOME_STATS_CACHE, RedisKeys.ADMIN_STATS_CACHE);
        });
        return decorate(recordMapper.selectBorrowById(record.getId()));
    }

    @Transactional
    public void returnBook(Long recordId, boolean admin) {
        BorrowRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw BizException.notFound("借阅记录不存在");
        }
        if (!admin && !record.getUserId().equals(UserContext.userId())) {
            throw BizException.forbidden("无权操作他人的借阅记录");
        }
        if (recordMapper.markReturned(recordId, LocalDateTime.now()) == 0) {
            throw BizException.conflict("该记录已归还，请勿重复操作");
        }
        // 馆藏总数被调小时，库存恢复以 total_copies 为上限，影响行数为 0 无需报错
        bookMapper.increaseStock(record.getBookId());
        afterCommit(() -> cache.delete(RedisKeys.HOME_STATS_CACHE, RedisKeys.ADMIN_STATS_CACHE));
    }

    @Transactional
    public BorrowVO renew(Long recordId) {
        BorrowRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw BizException.notFound("借阅记录不存在");
        }
        if (!record.getUserId().equals(UserContext.userId())) {
            throw BizException.forbidden("无权操作他人的借阅记录");
        }
        if (record.getStatus() == Constants.BORROW_RETURNED) {
            throw BizException.badRequest("该图书已归还，无需续借");
        }
        if (record.getDueTime().isBefore(LocalDateTime.now())) {
            throw BizException.badRequest("图书已逾期，无法续借，请先归还");
        }
        int maxRenew = properties.getBorrow().getMaxRenew();
        if (record.getRenewCount() != null && record.getRenewCount() >= maxRenew) {
            throw BizException.badRequest("最多续借 " + maxRenew + " 次");
        }
        int expected = record.getRenewCount() == null ? 0 : record.getRenewCount();
        int updated = recordMapper.renew(recordId,
                record.getDueTime().plusDays(properties.getBorrow().getRenewDays()), expected);
        if (updated == 0) {
            throw BizException.conflict("续借失败，记录状态已变化，请刷新后重试");
        }
        return decorate(recordMapper.selectBorrowById(recordId));
    }

    public PageResult<BorrowVO> myPage(long page, long size, Integer status) {
        return queryPage(page, size, UserContext.userId(), null, status);
    }

    public PageResult<BorrowVO> adminPage(long page, long size, String keyword, Integer status) {
        return queryPage(page, size, null, keyword, status);
    }

    public BorrowSummaryVO summary() {
        Long userId = UserContext.userId();
        BorrowSummaryVO vo = new BorrowSummaryVO();
        vo.setBorrowingCount(recordMapper.countActiveByUser(userId));
        vo.setOverdueCount(recordMapper.countOverdueByUser(userId));
        Long history = recordMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId));
        vo.setHistoryCount(history == null ? 0 : history);
        vo.setMaxBorrowing(properties.getBorrow().getMaxBorrowing());
        vo.setLoanDays(properties.getBorrow().getLoanDays());
        vo.setRenewDays(properties.getBorrow().getRenewDays());
        vo.setMaxRenew(properties.getBorrow().getMaxRenew());
        return vo;
    }

    private PageResult<BorrowVO> queryPage(long page, long size, Long userId, String keyword, Integer status) {
        Page<BorrowVO> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        recordMapper.selectBorrowPage(p, userId, keyword == null ? null : keyword.trim(), status);
        p.getRecords().forEach(this::decorate);
        return PageResult.of(p);
    }

    /**
     * 按应还日期实时计算剩余 / 逾期天数，并修正尚未被定时任务标记的逾期状态。
     */
    private BorrowVO decorate(BorrowVO vo) {
        if (vo == null || vo.getDueTime() == null) {
            return vo;
        }
        if (vo.getStatus() != null && vo.getStatus() == Constants.BORROW_RETURNED) {
            vo.setOverdueDays(0L);
            return vo;
        }
        LocalDate today = LocalDate.now();
        long daysLeft = ChronoUnit.DAYS.between(today, vo.getDueTime().toLocalDate());
        vo.setDaysLeft(daysLeft);
        vo.setOverdueDays(daysLeft < 0 ? -daysLeft : 0L);
        if (daysLeft < 0) {
            vo.setStatus(Constants.BORROW_OVERDUE);
        }
        return vo;
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
