package com.library.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录用户信息，保存在 Redis 中作为令牌的权威副本。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String role;
}
