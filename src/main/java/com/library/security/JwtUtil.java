package com.library.security;

import com.library.config.LibraryProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 的签发与解析。JWT 只承载身份标识，令牌是否有效以 Redis 中的副本为准，
 * 因此退出登录、管理员禁用账号、修改密码都能立即让旧令牌失效。
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey key;
    private final LibraryProperties properties;

    public JwtUtil(LibraryProperties properties) {
        this.properties = properties;
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
            log.warn("未配置 library.jwt.secret，已生成随机密钥：应用重启后所有令牌失效");
        } else {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("library.jwt.secret 长度至少需要 32 字节");
            }
            this.key = Keys.hmacShaKeyFor(bytes);
        }
    }

    public long expireSeconds() {
        return properties.getJwt().getExpireHours() * 3600L;
    }

    public String createToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds() * 1000);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 校验签名与有效期，返回 claims；无效时返回 null。
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
