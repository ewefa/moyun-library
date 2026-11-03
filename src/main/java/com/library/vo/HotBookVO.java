package com.library.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 热门借阅排行条目。
 */
@Data
public class HotBookVO implements Serializable {

    private Long bookId;

    private String title;

    private String author;

    private String categoryName;

    private Integer availableCopies;

    /** 累计借阅次数（排行榜分值） */
    private Integer borrowCount;
}
