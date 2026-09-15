package cn.edu.tju.takeout.recommend;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SearchHistoryMapper {
    @Insert("""
        INSERT INTO user_search_history(user_id, keyword, created_at)
        VALUES(#{userId}, #{keyword}, #{createdAt})
        """)
    void insert(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("createdAt") LocalDateTime createdAt);

    @Select("""
        SELECT keyword FROM user_search_history
        WHERE user_id = #{userId} ORDER BY id DESC LIMIT 1
        """)
    String findLatestKeyword(@Param("userId") Long userId);

    // 每个关键词的搜索次数与最近一次搜索时间，供推荐打分做时间衰减
    @Select("""
        SELECT keyword, COUNT(*) AS times, MAX(created_at) AS last_at
        FROM user_search_history
        WHERE user_id = #{userId}
        GROUP BY keyword
        ORDER BY times DESC, last_at DESC
        LIMIT #{limit}
        """)
    List<KeywordStat> findTopKeywords(
            @Param("userId") Long userId, @Param("limit") int limit);

    @Delete("DELETE FROM user_search_history WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    record KeywordStat(String keyword, int times, LocalDateTime lastAt) {}
}
