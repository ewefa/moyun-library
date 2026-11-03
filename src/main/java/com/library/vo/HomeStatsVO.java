package com.library.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 首页统计数据。
 */
@Data
public class HomeStatsVO implements Serializable {

    /** 在架图书种数 */
    private long totalBooks;

    /** 馆藏总册数 */
    private long totalCopies;

    /** 当前可借册数 */
    private long availableCopies;

    /** 当前在借册数 */
    private long borrowingCount;

    /** 读者人数 */
    private long readerCount;

    /** 今日借出册数 */
    private long todayBorrows;
}
