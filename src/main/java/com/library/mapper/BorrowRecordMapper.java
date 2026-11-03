package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.entity.BorrowRecord;
import com.library.vo.BorrowVO;
import com.library.vo.CategoryStatVO;
import com.library.vo.TrendItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    IPage<BorrowVO> selectBorrowPage(IPage<BorrowVO> page,
                                     @Param("userId") Long userId,
                                     @Param("keyword") String keyword,
                                     @Param("status") Integer status);

    BorrowVO selectBorrowById(@Param("id") Long id);

    /** 当前占用借阅额度的记录数（借阅中 + 已逾期） */
    @Select("SELECT COUNT(*) FROM t_borrow_record WHERE user_id = #{userId} AND status IN (0, 2)")
    int countActiveByUser(@Param("userId") Long userId);

    /** 以应还时间为准判断逾期，避免定时任务尚未落库时漏判 */
    @Select("SELECT COUNT(*) FROM t_borrow_record WHERE user_id = #{userId} AND status IN (0, 2) AND due_time < NOW()")
    int countOverdueByUser(@Param("userId") Long userId);

    /** 同一本书是否存在未归还记录 */
    @Select("SELECT COUNT(*) FROM t_borrow_record WHERE user_id = #{userId} AND book_id = #{bookId} AND status IN (0, 2)")
    int countActiveByBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /** 该书全馆未归还记录数（删除图书前校验） */
    @Select("SELECT COUNT(*) FROM t_borrow_record WHERE book_id = #{bookId} AND status IN (0, 2)")
    int countActiveByBookId(@Param("bookId") Long bookId);

    /**
     * 条件更新为已归还：只有未归还的记录会被更新，并发重复归还时第二次影响行数为 0。
     */
    @Update("""
            UPDATE t_borrow_record
            SET status = 1, return_time = #{returnTime}
            WHERE id = #{id} AND status IN (0, 2)
            """)
    int markReturned(@Param("id") Long id, @Param("returnTime") LocalDateTime returnTime);

    /**
     * 条件续借：未逾期且续借次数未被其他请求改动时才成功。
     */
    @Update("""
            UPDATE t_borrow_record
            SET due_time = #{newDueTime}, renew_count = renew_count + 1
            WHERE id = #{id} AND status = 0 AND due_time >= NOW() AND renew_count = #{expectedRenewCount}
            """)
    int renew(@Param("id") Long id, @Param("newDueTime") LocalDateTime newDueTime,
              @Param("expectedRenewCount") int expectedRenewCount);

    @Select("SELECT COUNT(*) FROM t_borrow_record WHERE status IN (0, 2)")
    long countBorrowing();

    @Select("SELECT COUNT(*) FROM t_borrow_record WHERE status = 2")
    long countOverdue();

    @Select("SELECT COUNT(*) FROM t_borrow_record WHERE DATE(borrow_time) = CURDATE()")
    long countTodayBorrows();

    @Select("SELECT COUNT(*) FROM t_borrow_record WHERE status = 1 AND DATE(return_time) = CURDATE()")
    long countTodayReturns();

    /** 把已过应还日期的借阅中记录标记为逾期 */
    @Update("UPDATE t_borrow_record SET status = 2 WHERE status = 0 AND due_time < NOW()")
    int markOverdue();

    /** 近 N 天每日借出量（含今天），无记录的日期补 0 */
    @Select("""
            WITH RECURSIVE d AS (
                SELECT 0 AS n
                UNION ALL
                SELECT n + 1 FROM d WHERE n < #{days} - 1
            )
            SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL d.n DAY), '%m-%d') AS date,
                   (SELECT COUNT(*) FROM t_borrow_record r
                    WHERE DATE(r.borrow_time) = DATE_SUB(CURDATE(), INTERVAL d.n DAY)) AS count
            FROM d
            ORDER BY d.n DESC
            """)
    List<TrendItem> selectDailyTrend(@Param("days") int days);

    /** 各分类图书种数 */
    @Select("""
            SELECT c.name AS name, COUNT(b.id) AS count
            FROM t_category c
            LEFT JOIN t_book b ON b.category_id = c.id AND b.deleted = 0
            WHERE c.deleted = 0
            GROUP BY c.id, c.name, c.sort
            ORDER BY c.sort ASC, c.id ASC
            """)
    List<CategoryStatVO> selectCategoryStats();
}
