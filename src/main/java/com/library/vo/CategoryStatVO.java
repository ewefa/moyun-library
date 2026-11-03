package com.library.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 分类图书数量统计。
 */
@Data
public class CategoryStatVO implements Serializable {

    private String name;

    private Long count;
}
