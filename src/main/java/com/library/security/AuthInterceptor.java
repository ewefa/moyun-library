package com.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.BizException;
import com.library.common.LoginUser;
import com.library.common.RedisKeys;
import com.library.common.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录与角色校验：
 * 1. 解析 Authorization: Bearer &lt;token&gt;，校验 JWT 签名与有效期；
 * 2. 到 Redis 查令牌副本，删除 / 过期 / 被踢下线的令牌立即失效；
 * 3. 根据 {@link RequireRole} 做角色校验，读者访问管理接口返回 403；
 * 4. 把登录用户写入 {@link UserContext}，请求结束后清理。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            throw BizException.unauthorized("请先登录");
        }
        String token = header.substring(PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw BizException.unauthorized("请先登录");
        }

        Claims claims = jwtUtil.parse(token);
        if (claims == null) {
            throw BizException.unauthorized("登录状态已失效，请重新登录");
        }

        String cached = redis.opsForValue().get(RedisKeys.token(token));
        if (cached == null) {
            throw BizException.unauthorized("登录状态已失效，请重新登录");
        }
        LoginUser user;
        try {
            user = objectMapper.readValue(cached, LoginUser.class);
        } catch (Exception e) {
            throw BizException.unauthorized("登录状态异常，请重新登录");
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null && !requireRole.value().equals(user.getRole())) {
            throw BizException.forbidden("没有权限执行该操作");
        }

        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
