package com.library.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器：把各类异常统一转换成 {@link Result}，避免堆栈信息泄露到前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数不合法");
        return Result.fail(Result.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("请求参数不合法");
        return Result.fail(Result.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException e) {
        return Result.fail(Result.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(Result.BAD_REQUEST, "缺少参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail(Result.BAD_REQUEST, "参数格式错误：" + e.getName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethod(HttpRequestMethodNotSupportedException e) {
        return Result.fail(Result.BAD_REQUEST, "不支持的请求方式：" + e.getMethod());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        return Result.fail(Result.NOT_FOUND, "接口或资源不存在");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicate(DuplicateKeyException e) {
        log.warn("唯一键冲突: {}", e.getMessage());
        return Result.fail(Result.CONFLICT, "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理的服务器异常", e);
        return Result.fail(Result.ERROR, "服务器开小差了，请稍后重试");
    }
}
