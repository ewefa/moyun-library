package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BizException;
import com.library.common.Constants;
import com.library.common.PageResult;
import com.library.common.RedisCache;
import com.library.common.RedisKeys;
import com.library.dto.BookRequest;
import com.library.entity.Book;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.CategoryMapper;
import com.library.vo.BookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 图书查询与后台维护。库存字段只能通过借还业务变更，编辑馆藏总数时按在借数量重新计算可借数。
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final RedisCache cache;

    public PageResult<BookVO> page(long page, long size, String keyword, Long categoryId,
                                   Integer status, String sort) {
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<BookVO> p = new Page<>(current, pageSize);
        bookMapper.selectBookPage(p, trim(keyword), categoryId, status, normalizeSort(sort));
        return PageResult.of(p);
    }

    public BookVO detail(Long id) {
        BookVO vo = bookMapper.selectBookVO(id);
        if (vo == null) {
            throw BizException.notFound("图书不存在");
        }
        return vo;
    }

    @Transactional
    public BookVO create(BookRequest request) {
        ensureCategory(request.getCategoryId());
        ensureIsbnUnique(request.getIsbn(), null);
        Book book = new Book();
        applyRequest(book, request);
        book.setAvailableCopies(request.getTotalCopies());
        book.setBorrowCount(0);
        book.setStatus(request.getStatus() == null ? Constants.BOOK_ON_SHELF : request.getStatus());
        bookMapper.insert(book);
        invalidateStats();
        return detail(book.getId());
    }

    @Transactional
    public BookVO update(Long id, BookRequest request) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw BizException.notFound("图书不存在");
        }
        ensureCategory(request.getCategoryId());
        ensureIsbnUnique(request.getIsbn(), id);

        int borrowed = book.getTotalCopies() - book.getAvailableCopies();
        if (request.getTotalCopies() < borrowed) {
            throw BizException.badRequest("馆藏册数不能小于当前在借册数 " + borrowed);
        }
        applyRequest(book, request);
        // 可借数 = 新馆藏总数 - 已借出数，保证借还过程中库存不会凭空增加
        book.setAvailableCopies(request.getTotalCopies() - borrowed);
        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        }
        bookMapper.updateById(book);
        invalidateStats();
        return detail(id);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != Constants.BOOK_ON_SHELF && status != Constants.BOOK_OFF_SHELF)) {
            throw BizException.badRequest("状态不合法");
        }
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw BizException.notFound("图书不存在");
        }
        book.setStatus(status);
        bookMapper.updateById(book);
        invalidateStats();
    }

    @Transactional
    public void delete(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw BizException.notFound("图书不存在");
        }
        int active = borrowRecordMapper.countActiveByBookId(id);
        if (active > 0) {
            throw BizException.conflict("该书还有 " + active + " 册未归还，无法删除");
        }
        bookMapper.deleteById(id);
        invalidateStats();
    }

    private void applyRequest(Book book, BookRequest request) {
        book.setIsbn(request.getIsbn().trim());
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setPublisher(trim(request.getPublisher()));
        book.setCategoryId(request.getCategoryId());
        book.setTotalCopies(request.getTotalCopies());
        book.setLocation(trim(request.getLocation()));
        book.setDescription(trim(request.getDescription()));
    }

    private void ensureCategory(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw BizException.badRequest("分类不存在");
        }
    }

    private void ensureIsbnUnique(String isbn, Long excludeId) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>().eq(Book::getIsbn, isbn.trim());
        if (excludeId != null) {
            wrapper.ne(Book::getId, excludeId);
        }
        Long count = bookMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BizException.conflict("ISBN 已存在");
        }
    }

    private void invalidateStats() {
        cache.delete(RedisKeys.HOME_STATS_CACHE, RedisKeys.ADMIN_STATS_CACHE,
                RedisKeys.CATEGORY_CACHE, RedisKeys.HOT_BOOK_CACHE);
    }

    private String normalizeSort(String sort) {
        if ("hot".equals(sort) || "new".equals(sort) || "title".equals(sort)) {
            return sort;
        }
        return "new";
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
