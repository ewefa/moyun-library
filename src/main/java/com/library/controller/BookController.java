package com.library.controller;

import com.library.common.Constants;
import com.library.common.PageResult;
import com.library.common.Result;
import com.library.service.BookService;
import com.library.service.HotRankService;
import com.library.vo.BookVO;
import com.library.vo.HotBookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开图书目录：只展示上架图书。
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final HotRankService hotRankService;

    @GetMapping
    public Result<PageResult<BookVO>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "12") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String sort) {
        return Result.ok(bookService.page(page, size, keyword, categoryId,
                Constants.BOOK_ON_SHELF, sort));
    }

    @GetMapping("/hot")
    public Result<List<HotBookVO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(hotRankService.top(Math.min(Math.max(limit, 1), 20)));
    }

    @GetMapping("/{id}")
    public Result<BookVO> detail(@PathVariable Long id) {
        return Result.ok(bookService.detail(id));
    }
}
