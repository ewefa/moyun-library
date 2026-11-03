package com.library.common;

/**
 * 业务异常。code 取值与 {@link Result} 保持一致。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(Result.BAD_REQUEST, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BizException badRequest(String message) {
        return new BizException(Result.BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(Result.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(Result.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(Result.NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(Result.CONFLICT, message);
    }
}
