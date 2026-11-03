package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图书。
 */
@Data
@TableName("t_book")
public class Book implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String isbn;

    private String title;

    private String author;

    private String publisher;

    private Long categoryId;

    /** 馆藏总册数 */
    private Integer totalCopies;

    /** 当前可借册数 */
    private Integer availableCopies;

    /** 馆藏位置，如 A区-3排 */
    private String location;

    /** 1 上架 0 下架 */
    private Integer status;

    private String description;

    /** 累计借阅次数（热门排行依据之一） */
    private Integer borrowCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer deleted;
}
