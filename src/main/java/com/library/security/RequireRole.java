package com.library.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 类或方法上，要求当前登录用户具备指定角色。
 * 未标注的受保护接口只要求登录。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    String value() default "ADMIN";
}
