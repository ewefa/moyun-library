package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.BizException;
import com.library.common.Constants;
import com.library.common.LoginUser;
import com.library.common.RedisKeys;
import com.library.common.UserContext;
import com.library.config.LibraryProperties;
import com.library.dto.ChangePasswordRequest;
import com.library.dto.LoginRequest;
import com.library.dto.ProfileRequest;
import com.library.dto.RegisterRequest;
import com.library.entity.User;
import com.library.mapper.UserMapper;
import com.library.security.JwtUtil;
import com.library.vo.LoginVO;
import com.library.vo.UserVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 注册、登录、令牌签发与账号自助维护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final LibraryProperties properties;

    @Transactional
    public LoginVO register(RegisterRequest request) {
        String username = request.getUsername().trim();
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw BizException.conflict("用户名已被注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setPhone(request.getPhone() == null ? "" : request.getPhone().trim());
        user.setRole(Constants.ROLE_READER);
        user.setStatus(Constants.USER_ENABLED);
        userMapper.insert(user);
        log.info("新读者注册: id={}, username={}", user.getId(), user.getUsername());
        return issueToken(user);
    }

    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername().trim()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != Constants.USER_ENABLED) {
            throw BizException.forbidden("账号已被禁用，请联系图书馆管理员");
        }
        return issueToken(user);
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Claims claims = jwtUtil.parse(token);
        redis.delete(RedisKeys.token(token));
        if (claims != null) {
            Long userId = Long.valueOf(claims.getSubject());
            String current = redis.opsForValue().get(RedisKeys.userToken(userId));
            if (token.equals(current)) {
                redis.delete(RedisKeys.userToken(userId));
            }
        }
    }

    public UserVO me() {
        User user = userMapper.selectById(UserContext.userId());
        if (user == null) {
            throw BizException.unauthorized("账号不存在，请重新登录");
        }
        return toVO(user);
    }

    public UserVO updateProfile(ProfileRequest request) {
        User user = userMapper.selectById(UserContext.userId());
        if (user == null) {
            throw BizException.unauthorized("账号不存在，请重新登录");
        }
        user.setNickname(request.getNickname().trim());
        user.setPhone(request.getPhone() == null ? "" : request.getPhone().trim());
        userMapper.updateById(user);
        // 同步刷新 Redis 中的昵称，避免页头信息过期
        refreshCachedUser(user);
        return toVO(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = userMapper.selectById(UserContext.userId());
        if (user == null) {
            throw BizException.unauthorized("账号不存在，请重新登录");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw BizException.badRequest("原密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BizException.badRequest("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        // 修改密码后所有旧令牌立即失效，要求重新登录
        revokeTokens(user.getId());
    }

    /**
     * 管理员重置密码后调用，踢掉该用户已登录的会话。
     */
    public void revokeTokens(Long userId) {
        String token = redis.opsForValue().get(RedisKeys.userToken(userId));
        if (token != null) {
            redis.delete(RedisKeys.token(token));
            redis.delete(RedisKeys.userToken(userId));
        }
    }

    private LoginVO issueToken(User user) {
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
        try {
            String previous = redis.opsForValue().get(RedisKeys.userToken(user.getId()));
            if (previous != null) {
                // 同一账号只保留最新会话
                redis.delete(RedisKeys.token(previous));
            }
            Duration ttl = Duration.ofSeconds(jwtUtil.expireSeconds());
            redis.opsForValue().set(RedisKeys.token(token), objectMapper.writeValueAsString(loginUser), ttl);
            redis.opsForValue().set(RedisKeys.userToken(user.getId()), token, ttl);
        } catch (Exception e) {
            log.error("写入登录令牌失败", e);
            throw new IllegalStateException("登录服务暂不可用，请稍后重试", e);
        }
        return new LoginVO(token, jwtUtil.expireSeconds(), toVO(user));
    }

    private void refreshCachedUser(User user) {
        try {
            String token = redis.opsForValue().get(RedisKeys.userToken(user.getId()));
            if (token == null) {
                return;
            }
            LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
            Long ttl = redis.getExpire(RedisKeys.token(token));
            if (ttl != null && ttl > 0) {
                redis.opsForValue().set(RedisKeys.token(token), objectMapper.writeValueAsString(loginUser),
                        Duration.ofSeconds(ttl));
            }
        } catch (Exception e) {
            log.warn("刷新令牌缓存中的用户信息失败: {}", e.getMessage());
        }
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
