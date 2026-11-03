package com.library.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.library.common.RedisCache;
import com.library.common.RedisKeys;
import com.library.entity.Book;
import com.library.entity.Category;
import com.library.mapper.BookMapper;
import com.library.mapper.CategoryMapper;
import com.library.vo.HotBookVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 热门借阅排行：用 Redis ZSet 累加借阅次数，取 Top N 时再回表补图书信息。
 * ZSet 丢失（Redis 重启 / 清库）时用数据库 borrow_count 自动重建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotRankService {

    private static final int CACHE_LIMIT = 10;

    private final StringRedisTemplate redis;
    private final RedisCache cache;
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 借阅成功后调用：榜单分值 +1，并让 Top 列表缓存失效。
     */
    public void recordBorrow(Long bookId) {
        try {
            redis.opsForZSet().incrementScore(RedisKeys.HOT_BOOK_RANK, String.valueOf(bookId), 1);
            cache.delete(RedisKeys.HOT_BOOK_CACHE, RedisKeys.HOME_STATS_CACHE, RedisKeys.ADMIN_STATS_CACHE);
        } catch (Exception e) {
            log.warn("更新热门借阅榜失败 bookId={}: {}", bookId, e.getMessage());
        }
    }

    public List<HotBookVO> top(int limit) {
        List<HotBookVO> cached = cache.get(RedisKeys.HOT_BOOK_CACHE, new TypeReference<>() {
        });
        if (cached != null) {
            return cached.size() > limit ? cached.subList(0, limit) : cached;
        }

        List<HotBookVO> result = loadFromRedis(limit);
        if (result == null) {
            result = rebuildFromDatabase(limit);
        }
        cache.set(RedisKeys.HOT_BOOK_CACHE, result, RedisKeys.HOT_CACHE_TTL);
        return result;
    }

    private List<HotBookVO> loadFromRedis(int limit) {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redis.opsForZSet().reverseRangeWithScores(RedisKeys.HOT_BOOK_RANK, 0, limit - 1);
            if (tuples == null || tuples.isEmpty()) {
                return null;
            }
            List<Long> ids = new ArrayList<>();
            Map<Long, Double> scores = new LinkedHashMap<>();
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null) {
                    continue;
                }
                Long id = Long.valueOf(tuple.getValue());
                ids.add(id);
                scores.put(id, tuple.getScore() == null ? 0 : tuple.getScore());
            }
            Map<Long, Book> books = new LinkedHashMap<>();
            if (!ids.isEmpty()) {
                for (Book book : bookMapper.selectBatchIds(ids)) {
                    books.put(book.getId(), book);
                }
            }
            Map<Long, String> categoryNames = categoryNames();
            List<HotBookVO> list = new ArrayList<>();
            for (Long id : ids) {
                Book book = books.get(id);
                if (book == null || book.getDeleted() != null && book.getDeleted() == 1) {
                    continue;
                }
                list.add(toVO(book, categoryNames.get(book.getCategoryId()),
                        (int) Math.round(scores.getOrDefault(id, 0D))));
            }
            return list;
        } catch (Exception e) {
            log.warn("读取 Redis 热门排行失败，降级查库: {}", e.getMessage());
            return null;
        }
    }

    private List<HotBookVO> rebuildFromDatabase(int limit) {
        List<Book> books = bookMapper.selectBorrowRank(Math.max(limit, CACHE_LIMIT));
        Map<Long, String> categoryNames = categoryNames();
        List<HotBookVO> list = new ArrayList<>();
        try {
            for (Book book : books) {
                if (book.getBorrowCount() != null && book.getBorrowCount() > 0) {
                    redis.opsForZSet().add(RedisKeys.HOT_BOOK_RANK, String.valueOf(book.getId()), book.getBorrowCount());
                }
            }
        } catch (Exception e) {
            log.warn("重建 Redis 热门排行失败: {}", e.getMessage());
        }
        for (Book book : books) {
            list.add(toVO(book, categoryNames.get(book.getCategoryId()),
                    book.getBorrowCount() == null ? 0 : book.getBorrowCount()));
            if (list.size() >= limit) {
                break;
            }
        }
        return list;
    }

    private Map<Long, String> categoryNames() {
        Map<Long, String> map = new LinkedHashMap<>();
        for (Category category : categoryMapper.selectList(null)) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }

    private HotBookVO toVO(Book book, String categoryName, int borrowCount) {
        HotBookVO vo = new HotBookVO();
        vo.setBookId(book.getId());
        vo.setTitle(book.getTitle());
        vo.setAuthor(book.getAuthor());
        vo.setCategoryName(categoryName);
        vo.setAvailableCopies(book.getAvailableCopies());
        vo.setBorrowCount(borrowCount);
        return vo;
    }
}
