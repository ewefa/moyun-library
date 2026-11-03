package com.library.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 读者借阅概况。
 */
@Data
public class BorrowSummaryVO implements Serializable {

    private int borrowingCount;

    private int overdueCount;

    private long historyCount;

    private int maxBorrowing;

    private int loanDays;

    private int renewDays;

    private int maxRenew;
}
