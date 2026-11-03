package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BizException;
import com.library.common.Constants;
import com.library.common.PageResult;
import com.library.common.UserContext;
import com.library.dto.ResetPasswordRequest;
import com.library.entity.User;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.UserMapper;
import com.library.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端用户管理：列表、启用 / 禁用、重置密码。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BorrowRecordMapper recordMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public PageResult<UserVO> page(long page, long size, String keyword, String role, Integer status) {
        Page<User> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        String kw = keyword == null ? null : keyword.trim();
        if (kw != null && !kw.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw));
        }
        wrapper.orderByDesc(User::getId);
        userMapper.selectPage(p, wrapper);
        Page<UserVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        voPage.setRecords(p.getRecords().stream().map(this::toVO).toList());
        return PageResult.of(voPage);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != Constants.USER_ENABLED && status != Constants.USER_DISABLED)) {
            throw BizException.badRequest("状态不合法");
        }
        if (id.equals(UserContext.userId())) {
            throw BizException.badRequest("不能禁用当前登录的账号");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        if (status == Constants.USER_DISABLED) {
            // 禁用后立即失效其令牌，无需等待 JWT 自然过期
            authService.revokeTokens(id);
        }
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        authService.revokeTokens(id);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setBorrowingCount(recordMapper.countActiveByUser(user.getId()));
        vo.setOverdueCount(recordMapper.countOverdueByUser(user.getId()));
        return vo;
    }
}
