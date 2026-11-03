package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.BorrowRequest;
import com.library.service.BorrowService;
import com.library.vo.BorrowSummaryVO;
import com.library.vo.BorrowVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 读者借阅接口：借书、还书、续借、我的借阅。
 */
@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @PostMapping
    public Result<BorrowVO> borrow(@Valid @RequestBody BorrowRequest request) {
        return Result.ok("借阅成功", borrowService.borrow(request.getBookId()));
    }

    @PutMapping("/{id}/return")
    public Result<Void> returnBook(@PathVariable Long id) {
        borrowService.returnBook(id, false);
        return Result.ok("归还成功", null);
    }

    @PutMapping("/{id}/renew")
    public Result<BorrowVO> renew(@PathVariable Long id) {
        return Result.ok("续借成功", borrowService.renew(id));
    }

    @GetMapping("/my")
    public Result<PageResult<BorrowVO>> my(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(borrowService.myPage(page, size, status));
    }

    @GetMapping("/summary")
    public Result<BorrowSummaryVO> summary() {
        return Result.ok(borrowService.summary());
    }
}
