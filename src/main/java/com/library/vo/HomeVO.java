package com.library.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 首页聚合数据。
 */
@Data
public class HomeVO implements Serializable {

    private HomeStatsVO stats;

    private List<HotBookVO> hotBooks;

    private List<BookVO> newBooks;
}
