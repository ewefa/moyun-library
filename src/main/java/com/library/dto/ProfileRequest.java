package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileRequest {

    @NotBlank(message = "请输入昵称")
    @Size(max = 20, message = "昵称最多 20 个字符")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
