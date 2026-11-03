package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借阅记录。物理保留全部历史，不做逻辑删除。
 */
@Data
@TableName("t_borrow_record")
public class BorrowRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    /** 借出时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime borrowTime;

    /** 应归还时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;

    /** 实际归还时间，未归还为空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime returnTime;

    /** 已续借次数 */
    private Integer renewCount;

    /** 0 借阅中 1 已归还 2 已逾期 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
