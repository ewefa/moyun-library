package com.library.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private long page;

    /** 每页条数 */
    private long size;

    /** 总页数 */
    private long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        return new PageResult<>(records, total, page, size, pages);
    }
}
