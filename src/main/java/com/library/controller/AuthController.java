package com.library.controller;

import com.library.common.Result;
import com.library.dto.ChangePasswordRequest;
import com.library.dto.LoginRequest;
import com.library.dto.ProfileRequest;
import com.library.dto.RegisterRequest;
import com.library.service.AuthService;
import com.library.vo.LoginVO;
import com.library.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录注册与账号自助接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok("登录成功", authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok("注册成功", authService.register(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim() : null;
        authService.logout(token);
        return Result.ok("已退出登录", null);
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.me());
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody ProfileRequest request) {
        return Result.ok("资料已更新", authService.updateProfile(request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.ok("密码已修改，请重新登录", null);
    }
}
