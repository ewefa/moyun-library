package com.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "请输入分类名称")
    @Size(max = 20, message = "分类名称最多 20 个字符")
    private String name;

    @Min(value = 0, message = "排序值不能为负数")
    @Max(value = 9999, message = "排序值过大")
    private Integer sort;

    @Size(max = 200, message = "分类说明最多 200 个字符")
    private String description;
}
