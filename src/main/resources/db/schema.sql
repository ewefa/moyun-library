-- 在线图书借阅管理系统 表结构
-- 说明：使用 CREATE TABLE IF NOT EXISTS，配合 spring.sql.init.mode=always，可重复执行。

CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(32)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    nickname    VARCHAR(32)  NOT NULL COMMENT '昵称',
    role        VARCHAR(16)  NOT NULL DEFAULT 'READER' COMMENT 'ADMIN/READER',
    phone       VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_role_status (role, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户';

CREATE TABLE IF NOT EXISTS t_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32)  NOT NULL COMMENT '分类名称',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '描述',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_category_name (name),
    KEY idx_category_sort (sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图书分类';

CREATE TABLE IF NOT EXISTS t_book (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    isbn             VARCHAR(20)   NOT NULL COMMENT 'ISBN',
    title            VARCHAR(80)   NOT NULL COMMENT '书名',
    author           VARCHAR(60)   NOT NULL COMMENT '作者',
    publisher        VARCHAR(60)   NOT NULL DEFAULT '' COMMENT '出版社',
    category_id      BIGINT        NOT NULL COMMENT '分类 ID',
    total_copies     INT           NOT NULL DEFAULT 1 COMMENT '馆藏总册数',
    available_copies INT           NOT NULL DEFAULT 1 COMMENT '当前可借册数',
    location         VARCHAR(40)   NOT NULL DEFAULT '' COMMENT '馆藏位置',
    status           TINYINT       NOT NULL DEFAULT 1 COMMENT '1 上架 0 下架',
    description      VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '简介',
    borrow_count     INT           NOT NULL DEFAULT 0 COMMENT '累计借阅次数',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_book_isbn (isbn),
    KEY idx_book_category (category_id),
    KEY idx_book_title (title),
    KEY idx_book_status (status),
    KEY idx_book_borrow_count (borrow_count),
    CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES t_category (id),
    CONSTRAINT chk_book_stock CHECK (total_copies >= 1 AND available_copies >= 0 AND available_copies <= total_copies)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图书';

CREATE TABLE IF NOT EXISTS t_borrow_record (
    id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT   NOT NULL COMMENT '读者 ID',
    book_id      BIGINT   NOT NULL COMMENT '图书 ID',
    borrow_time  DATETIME NOT NULL COMMENT '借出时间',
    due_time     DATETIME NOT NULL COMMENT '应归还时间',
    return_time  DATETIME NULL COMMENT '实际归还时间',
    renew_count  INT      NOT NULL DEFAULT 0 COMMENT '已续借次数',
    status       TINYINT  NOT NULL DEFAULT 0 COMMENT '0 借阅中 1 已归还 2 已逾期',
    active_flag  TINYINT GENERATED ALWAYS AS (IF(status IN (0, 2), 1, NULL)) STORED COMMENT '未归还标记，仅用于唯一索引',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_borrow_user_book_active (user_id, book_id, active_flag),
    KEY idx_borrow_user_status (user_id, status),
    KEY idx_borrow_book_status (book_id, status),
    KEY idx_borrow_due_time (due_time),
    KEY idx_borrow_status (status),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES t_book (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '借阅记录';
