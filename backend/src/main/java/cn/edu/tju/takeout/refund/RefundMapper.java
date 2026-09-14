package cn.edu.tju.takeout.refund;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RefundMapper {
    @Insert("""
            INSERT INTO refund_requests(
                order_id, user_id, amount, reason, evidence_urls, status, created_at)
            VALUES(
                #{orderId}, #{userId}, #{amount}, #{reason}, #{evidenceUrls},
                #{status}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RefundRequest refund);

    @Select("""
            SELECT COALESCE(SUM(amount), 0) FROM refund_requests
            WHERE order_id = #{orderId} AND status IN ('PENDING', 'APPROVED')
            """)
    BigDecimal reserved(@Param("orderId") Long orderId);

    @Select("SELECT * FROM refund_requests WHERE user_id = #{userId} ORDER BY id DESC")
    List<RefundRequest> findByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM refund_requests WHERE id = #{id}")
    Optional<RefundRequest> findById(@Param("id") Long id);

    @Select("""
            SELECT * FROM refund_requests
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<RefundRequest> findPageForAdmin(
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM refund_requests")
    long countAll();

    @Select("""
            SELECT r.* FROM refund_requests r
            JOIN orders o ON o.id = r.order_id
            JOIN shops s ON s.id = o.shop_id
            WHERE s.merchant_id = #{merchantId}
            ORDER BY r.id DESC
            """)
    List<RefundRequest> findByMerchant(@Param("merchantId") Long merchantId);

    @Update("""
            UPDATE refund_requests
            SET status = #{status}, handled_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 'PENDING'
              AND order_id IN (
                  SELECT o.id FROM orders o
                  JOIN shops s ON s.id = o.shop_id
                  WHERE s.merchant_id = #{merchantId}
              )
            """)
    int decideForMerchant(
            @Param("id") Long id,
            @Param("merchantId") Long merchantId,
            @Param("status") String status);

    @Update("""
            UPDATE refund_requests
            SET status = #{status}, handled_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 'PENDING'
            """)
    int decide(@Param("id") Long id, @Param("status") String status);
}
