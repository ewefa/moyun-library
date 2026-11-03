package com.library.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 最近 7 天借阅趋势条目。
 */
@Data
public class TrendItem implements Serializable {

    /** MM-dd */
    private String date;

    private Long count;
}
