package com.library.controller;

import com.library.common.Result;
import com.library.service.StatService;
import com.library.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页聚合数据（统计 + 热门排行 + 新书）。
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final StatService statService;

    @GetMapping
    public Result<HomeVO> home() {
        return Result.ok(statService.home());
    }
}
