package com.library.common;

import java.time.Duration;

/**
 * Redis Key 统一管理，避免散落在业务代码中的字符串。
 */
public final class RedisKeys {

    /** 令牌 -> 登录用户（JSON），过期时间与 JWT 一致 */
    public static final String TOKEN_PREFIX = "library:token:token:";

    /** 用户 ID -> 当前有效令牌，用于改密 / 禁用 / 重新登录时踢掉旧令牌 */
    public static final String USER_TOKEN_PREFIX = "library:token:user:";

    /** 热门图书借阅排行（ZSet，member=bookId，score=借阅次数） */
    public static final String HOT_BOOK_RANK = "library:rank:borrow";

    /** 热门排行展示数据缓存（JSON） */
    public static final String HOT_BOOK_CACHE = "library:cache:hot:top";

    /** 首页统计数据缓存（JSON） */
    public static final String HOME_STATS_CACHE = "library:cache:home:stats";

    /** 分类列表缓存（JSON） */
    public static final String CATEGORY_CACHE = "library:cache:categories";

    /** 管理端看板缓存（JSON） */
    public static final String ADMIN_STATS_CACHE = "library:cache:admin:stats";

    public static final Duration HOT_CACHE_TTL = Duration.ofMinutes(5);
    public static final Duration HOME_STATS_TTL = Duration.ofSeconds(60);
    public static final Duration CATEGORY_TTL = Duration.ofMinutes(10);
    public static final Duration ADMIN_STATS_TTL = Duration.ofSeconds(30);

    private RedisKeys() {
    }

    public static String token(String token) {
        return TOKEN_PREFIX + token;
    }

    public static String userToken(Long userId) {
        return USER_TOKEN_PREFIX + userId;
    }
}
