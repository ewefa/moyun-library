package com.library.controller;

import com.library.common.Result;
import com.library.service.CategoryService;
import com.library.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开分类列表。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.list());
    }
}
