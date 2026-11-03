package com.library.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图书展示对象，附带分类名称。
 */
@Data
public class BookVO implements Serializable {

    private Long id;

    private String isbn;

    private String title;

    private String author;

    private String publisher;

    private Long categoryId;

    private String categoryName;

    private Integer totalCopies;

    private Integer availableCopies;

    private String location;

    private Integer status;

    private String description;

    private Integer borrowCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
