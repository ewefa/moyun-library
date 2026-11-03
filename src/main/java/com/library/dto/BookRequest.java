package com.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookRequest {

    @NotBlank(message = "请输入 ISBN")
    @Pattern(regexp = "^[0-9Xx-]{10,20}$", message = "ISBN 需为 10-20 位数字、X 或连字符")
    private String isbn;

    @NotBlank(message = "请输入书名")
    @Size(max = 80, message = "书名最多 80 个字符")
    private String title;

    @NotBlank(message = "请输入作者")
    @Size(max = 60, message = "作者最多 60 个字符")
    private String author;

    @Size(max = 60, message = "出版社最多 60 个字符")
    private String publisher;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotNull(message = "请输入馆藏册数")
    @Min(value = 1, message = "馆藏册数至少为 1")
    @Max(value = 999, message = "馆藏册数最多为 999")
    private Integer totalCopies;

    @Size(max = 40, message = "馆藏位置最多 40 个字符")
    private String location;

    @Min(value = 0, message = "状态不合法")
    @Max(value = 1, message = "状态不合法")
    private Integer status;

    @Size(max = 1000, message = "简介最多 1000 个字符")
    private String description;
}
