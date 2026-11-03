package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.Category;
import com.library.vo.CategoryVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 分类列表，附带每个分类下未删除图书的数量。
     */
    @Select("""
            SELECT c.id, c.name, c.sort, c.description, COUNT(b.id) AS book_count
            FROM t_category c
            LEFT JOIN t_book b ON b.category_id = c.id AND b.deleted = 0
            WHERE c.deleted = 0
            GROUP BY c.id, c.name, c.sort, c.description
            ORDER BY c.sort ASC, c.id ASC
            """)
    List<CategoryVO> selectWithBookCount();
}
