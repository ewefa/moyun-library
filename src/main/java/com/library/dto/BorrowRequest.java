package com.library.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowRequest {

    @NotNull(message = "请选择要借阅的图书")
    private Long bookId;
}
