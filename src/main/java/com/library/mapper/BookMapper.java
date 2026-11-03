package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.entity.Book;
import com.library.vo.BookVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface BookMapper extends BaseMapper<Book> {

    /**
     * 图书分页（附带分类名称）。sort 只接受 hot / new / title，其余按入库时间倒序。
     */
    IPage<BookVO> selectBookPage(IPage<BookVO> page,
                                 @Param("keyword") String keyword,
                                 @Param("categoryId") Long categoryId,
                                 @Param("status") Integer status,
                                 @Param("sort") String sort);

    BookVO selectBookVO(@Param("id") Long id);

    /**
     * 原子扣减库存并累加借阅次数：available_copies > 0 才更新，防止并发超借。
     */
    @Update("""
            UPDATE t_book
            SET available_copies = available_copies - 1,
                borrow_count = borrow_count + 1
            WHERE id = #{id} AND available_copies > 0 AND deleted = 0
            """)
    int decreaseStock(@Param("id") Long id);

    /**
     * 归还时恢复库存，最多恢复到馆藏总数。
     */
    @Update("""
            UPDATE t_book
            SET available_copies = available_copies + 1
            WHERE id = #{id} AND available_copies < total_copies AND deleted = 0
            """)
    int increaseStock(@Param("id") Long id);

    /**
     * Redis 排行缺失时，用数据库累计借阅次数重建榜单。
     */
    @Select("""
            SELECT * FROM t_book
            WHERE deleted = 0
            ORDER BY borrow_count DESC, id ASC
            LIMIT #{limit}
            """)
    List<Book> selectBorrowRank(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM t_book WHERE deleted = 0 AND status = 1")
    long countOnShelf();

    @Select("SELECT COALESCE(SUM(total_copies), 0) FROM t_book WHERE deleted = 0 AND status = 1")
    long sumTotalCopies();

    @Select("SELECT COALESCE(SUM(available_copies), 0) FROM t_book WHERE deleted = 0 AND status = 1")
    long sumAvailableCopies();
}
