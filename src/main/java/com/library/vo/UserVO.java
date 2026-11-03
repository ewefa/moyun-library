package com.library.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息（不含密码）。
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String role;

    private String phone;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 当前在借册数（管理端列表使用） */
    private Integer borrowingCount;

    /** 当前逾期册数（管理端列表使用） */
    private Integer overdueCount;
}
