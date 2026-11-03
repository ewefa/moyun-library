package com.library.config;

import com.library.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 接口鉴权范围配置：图书目录、分类、首页统计和登录注册开放，其余 /api 接口需要登录。
 * 管理端接口另由 {@code @RequireRole("ADMIN")} 限制角色。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/books",
                        "/api/books/**",
                        "/api/categories",
                        "/api/home/**"
                );
    }
}
