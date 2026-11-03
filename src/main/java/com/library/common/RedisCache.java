package com.library.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 简单的 JSON 缓存读写封装。缓存只是加速手段，Redis 异常时降级为直接查库，
 * 不影响业务主流程（登录鉴权另有强依赖，见 AuthInterceptor）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public <T> T get(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (DataAccessException e) {
            log.warn("读取缓存失败 key={}: {}", key, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("反序列化缓存失败 key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public <T> T get(String key, TypeReference<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (DataAccessException e) {
            log.warn("读取缓存失败 key={}: {}", key, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("反序列化缓存失败 key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("写入缓存失败 key={}: {}", key, e.getMessage());
        }
    }

    public void delete(String... keys) {
        try {
            for (String key : keys) {
                redis.delete(key);
            }
        } catch (DataAccessException e) {
            log.warn("删除缓存失败: {}", e.getMessage());
        }
    }
}
