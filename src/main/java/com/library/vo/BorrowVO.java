package com.library.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借阅记录展示对象，附带读者与图书信息。
 */
@Data
public class BorrowVO implements Serializable {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private Long bookId;

    private String bookTitle;

    private String bookAuthor;

    private String isbn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime borrowTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime returnTime;

    private Integer renewCount;

    /** 0 借阅中 1 已归还 2 已逾期 */
    private Integer status;

    /** 距应还日剩余天数，已逾期为负数；已归还为 null */
    private Long daysLeft;

    /** 逾期天数，未逾期为 0 */
    private Long overdueDays;
}
