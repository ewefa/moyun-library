package com.library.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果。HTTP 状态统一返回 200，业务状态放在 code 中：
 * 200-成功 400-参数错误 401-未登录/登录失效 403-无权限 404-资源不存在
 * 409-数据冲突 429-请求过于频繁 500-服务器异常
 */
@Data
public class Result<T> implements Serializable {

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int ERROR = 500;

    private int code;

    private String message;

    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(SUCCESS, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(SUCCESS, message, data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(BAD_REQUEST, message, null);
    }
}
