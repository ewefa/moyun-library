package com.library.common;

/**
 * 角色与状态常量。
 */
public final class Constants {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_READER = "READER";

    /** 用户：1 正常，0 禁用 */
    public static final int USER_ENABLED = 1;
    public static final int USER_DISABLED = 0;

    /** 图书：1 上架，0 下架 */
    public static final int BOOK_ON_SHELF = 1;
    public static final int BOOK_OFF_SHELF = 0;

    /** 借阅记录：0 借阅中，1 已归还，2 已逾期 */
    public static final int BORROW_ACTIVE = 0;
    public static final int BORROW_RETURNED = 1;
    public static final int BORROW_OVERDUE = 2;

    private Constants() {
    }
}
