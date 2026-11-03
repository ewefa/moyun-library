package com.library.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 分类展示对象，管理端附带图书数量。
 */
@Data
public class CategoryVO implements Serializable {

    private Long id;

    private String name;

    private Integer sort;

    private String description;

    private Long bookCount;
}
