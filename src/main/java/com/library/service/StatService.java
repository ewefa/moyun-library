package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.library.common.Constants;
import com.library.common.RedisCache;
import com.library.common.RedisKeys;
import com.library.entity.User;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.UserMapper;
import com.library.vo.AdminStatsVO;
import com.library.vo.BookVO;
import com.library.vo.HomeStatsVO;
import com.library.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 首页与管理端统计。统计类查询较重，统一走 Redis 短时缓存，数据变动时主动失效。
 */
@Service
@RequiredArgsConstructor
public class StatService {

    private static final int TREND_DAYS = 7;
    private static final int HOT_LIMIT = 10;
    private static final int NEW_BOOKS = 8;

    private final BookMapper bookMapper;
    private final BorrowRecordMapper recordMapper;
    private final UserMapper userMapper;
    private final HotRankService hotRankService;
    private final RedisCache cache;

    public HomeVO home() {
        HomeVO cached = cache.get(RedisKeys.HOME_STATS_CACHE, new TypeReference<>() {
        });
        if (cached != null) {
            return cached;
        }
        HomeVO vo = new HomeVO();
        HomeStatsVO stats = new HomeStatsVO();
        stats.setTotalBooks(bookMapper.countOnShelf());
        stats.setTotalCopies(bookMapper.sumTotalCopies());
        stats.setAvailableCopies(bookMapper.sumAvailableCopies());
        stats.setBorrowingCount(recordMapper.countBorrowing());
        stats.setReaderCount(readerCount());
        stats.setTodayBorrows(recordMapper.countTodayBorrows());
        vo.setStats(stats);
        vo.setHotBooks(hotRankService.top(HOT_LIMIT));

        Page<BookVO> newPage = new Page<>(1, NEW_BOOKS);
        bookMapper.selectBookPage(newPage, null, null, Constants.BOOK_ON_SHELF, "new");
        vo.setNewBooks(newPage.getRecords());

        cache.set(RedisKeys.HOME_STATS_CACHE, vo, RedisKeys.HOME_STATS_TTL);
        return vo;
    }

    public AdminStatsVO adminStats() {
        AdminStatsVO cached = cache.get(RedisKeys.ADMIN_STATS_CACHE, new TypeReference<>() {
        });
        if (cached != null) {
            return cached;
        }
        AdminStatsVO vo = new AdminStatsVO();
        vo.setTotalBooks(bookMapper.countOnShelf());
        vo.setTotalCopies(bookMapper.sumTotalCopies());
        vo.setAvailableCopies(bookMapper.sumAvailableCopies());
        vo.setBorrowingCount(recordMapper.countBorrowing());
        vo.setReaderCount(readerCount());
        vo.setOverdueCount(recordMapper.countOverdue());
        vo.setTodayBorrows(recordMapper.countTodayBorrows());
        vo.setTodayReturns(recordMapper.countTodayReturns());
        vo.setTrend(recordMapper.selectDailyTrend(TREND_DAYS));
        vo.setCategoryStats(recordMapper.selectCategoryStats());
        vo.setHotBooks(hotRankService.top(HOT_LIMIT));
        cache.set(RedisKeys.ADMIN_STATS_CACHE, vo, RedisKeys.ADMIN_STATS_TTL);
        return vo;
    }

    public void invalidate() {
        cache.delete(RedisKeys.HOME_STATS_CACHE, RedisKeys.ADMIN_STATS_CACHE);
    }

    private long readerCount() {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, Constants.ROLE_READER)
                .eq(User::getStatus, Constants.USER_ENABLED));
        return count == null ? 0 : count;
    }
}
