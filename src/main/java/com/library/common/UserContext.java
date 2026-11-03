package com.library.common;

/**
 * 当前请求的登录用户上下文，由鉴权拦截器写入并在请求结束时清理。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("请先登录");
        }
        return user;
    }

    public static Long userId() {
        return require().getId();
    }

    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && "ADMIN".equals(user.getRole());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
