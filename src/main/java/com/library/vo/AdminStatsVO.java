package com.library.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 管理端数据看板。
 */
@Data
public class AdminStatsVO implements Serializable {

    private long totalBooks;

    private long totalCopies;

    private long availableCopies;

    private long borrowingCount;

    private long readerCount;

    private long overdueCount;

    private long todayBorrows;

    private long todayReturns;

    /** 近 7 天借阅趋势 */
    private List<TrendItem> trend;

    /** 分类图书分布 */
    private List<CategoryStatVO> categoryStats;

    /** 热门借阅 Top 10 */
    private List<HotBookVO> hotBooks;
}
