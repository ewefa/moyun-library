package com.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录成功返回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    private String token;

    /** 令牌有效期（秒） */
    private long expiresIn;

    private UserVO user;
}
