package com.library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 借阅与令牌相关的可配置参数。
 */
@Data
@ConfigurationProperties(prefix = "library")
public class LibraryProperties {

    private Jwt jwt = new Jwt();

    private Borrow borrow = new Borrow();

    @Data
    public static class Jwt {
        /** HS256 密钥，至少 32 字节 */
        private String secret;
        /** 令牌有效期（小时），同时作为 Redis 缓存过期时间 */
        private int expireHours = 24;
    }

    @Data
    public static class Borrow {
        /** 单次借阅天数 */
        private int loanDays = 30;
        /** 单次续借延长天数 */
        private int renewDays = 30;
        /** 最多续借次数 */
        private int maxRenew = 2;
        /** 每位读者最多同时借阅册数 */
        private int maxBorrowing = 5;
    }
}
