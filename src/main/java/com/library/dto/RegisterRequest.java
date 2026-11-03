package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "请输入用户名")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "用户名需为 4-20 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String password;

    @NotBlank(message = "请输入昵称")
    @Size(max = 20, message = "昵称最多 20 个字符")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
