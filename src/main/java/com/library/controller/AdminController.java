package com.library.controller;

import com.library.common.Constants;
import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.BookRequest;
import com.library.dto.CategoryRequest;
import com.library.dto.ResetPasswordRequest;
import com.library.dto.StatusRequest;
import com.library.security.RequireRole;
import com.library.service.BookService;
import com.library.service.BorrowService;
import com.library.service.CategoryService;
import com.library.service.StatService;
import com.library.service.UserService;
import com.library.vo.AdminStatsVO;
import com.library.vo.BookVO;
import com.library.vo.BorrowVO;
import com.library.vo.CategoryVO;
import com.library.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端接口：图书 / 分类 / 借阅 / 用户 / 统计。
 * 类上的 @RequireRole 由 AuthInterceptor 统一校验，读者访问一律 403。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole(Constants.ROLE_ADMIN)
public class AdminController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final BorrowService borrowService;
    private final UserService userService;
    private final StatService statService;

    // ---------- 图书 ----------

    @GetMapping("/books")
    public Result<PageResult<BookVO>> books(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(bookService.page(page, size, keyword, categoryId, status, "new"));
    }

    @PostMapping("/books")
    public Result<BookVO> createBook(@Valid @RequestBody BookRequest request) {
        return Result.ok("图书已新增", bookService.create(request));
    }

    @PutMapping("/books/{id}")
    public Result<BookVO> updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return Result.ok("图书已更新", bookService.update(id, request));
    }

    @DeleteMapping("/books/{id}")
    public Result<Void> deleteBook(@PathVariable Long id) {
        bookService.delete(id);
        return Result.ok("图书已删除", null);
    }

    @PutMapping("/books/{id}/status")
    public Result<Void> updateBookStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        bookService.updateStatus(id, request.getStatus());
        return Result.ok(request.getStatus() == Constants.BOOK_ON_SHELF ? "已上架" : "已下架", null);
    }

    // ---------- 分类 ----------

    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(categoryService.list());
    }

    @PostMapping("/categories")
    public Result<CategoryVO> createCategory(@Valid @RequestBody CategoryRequest request) {
        return Result.ok("分类已新增", categoryService.create(request));
    }

    @PutMapping("/categories/{id}")
    public Result<CategoryVO> updateCategory(@PathVariable Long id,
                                             @Valid @RequestBody CategoryRequest request) {
        return Result.ok("分类已更新", categoryService.update(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok("分类已删除", null);
    }

    // ---------- 借阅 ----------

    @GetMapping("/borrows")
    public Result<PageResult<BorrowVO>> borrows(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        return Result.ok(borrowService.adminPage(page, size, keyword, status));
    }

    @PutMapping("/borrows/{id}/return")
    public Result<Void> returnBook(@PathVariable Long id) {
        borrowService.returnBook(id, true);
        return Result.ok("已登记归还", null);
    }

    // ---------- 用户 ----------

    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String role,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(userService.page(page, size, keyword, role, status));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return Result.ok(request.getStatus() == Constants.USER_ENABLED ? "账号已启用" : "账号已禁用", null);
    }

    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return Result.ok("密码已重置", null);
    }

    // ---------- 统计 ----------

    @GetMapping("/stats")
    public Result<AdminStatsVO> stats() {
        return Result.ok(statService.adminStats());
    }
}
