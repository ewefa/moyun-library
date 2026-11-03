package com.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusRequest {

    @NotNull(message = "请传入状态")
    @Min(value = 0, message = "状态不合法")
    @Max(value = 1, message = "状态不合法")
    private Integer status;
}
