package cn.edu.tju.takeout.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper {
    @Insert("""
            INSERT INTO orders(order_no, user_id, shop_id, total_amount, status, address,
              recipient_name, recipient_phone, delivery_address, payment_status, payment_deadline, paid_at, rider_id, created_at)
            VALUES(#{orderNo}, #{userId}, #{shopId}, #{totalAmount}, #{status}, #{address},
              #{recipientName}, #{recipientPhone}, #{deliveryAddress}, #{paymentStatus}, #{paymentDeadline}, #{paidAt}, #{riderId}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);

    @Insert("""
            INSERT INTO order_items(
                order_id, product_id, product_name, unit_price, quantity, subtotal)
            VALUES(#{orderId}, #{productId}, #{productName}, #{unitPrice}, #{quantity}, #{subtotal})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertItem(OrderItem item);

    @Select("""
            <script>
            SELECT * FROM orders WHERE user_id = #{userId}
            <if test='status != null and status != ""'> AND status = #{status} </if>
            <if test='startTime != null'> AND created_at &gt;= #{startTime} </if>
            <if test='endTime != null'> AND created_at &lt;= #{endTime} </if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Order> findPageByUserId(
            Long userId, String status, LocalDateTime startTime, LocalDateTime endTime,
            Integer limit, Integer offset);

    @Select("""
            <script>
            SELECT COUNT(*) FROM orders WHERE user_id = #{userId}
            <if test='status != null and status != ""'> AND status = #{status} </if>
            <if test='startTime != null'> AND created_at &gt;= #{startTime} </if>
            <if test='endTime != null'> AND created_at &lt;= #{endTime} </if>
            </script>
            """)
    long countByUserId(
            Long userId, String status, LocalDateTime startTime, LocalDateTime endTime);

    @Select("""
            <script>
            SELECT * FROM orders WHERE shop_id = #{shopId}
            <if test='status != null and status != ""'> AND status = #{status} </if>
            <if test='startTime != null'> AND created_at &gt;= #{startTime} </if>
            <if test='endTime != null'> AND created_at &lt;= #{endTime} </if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Order> findPageByShopId(
            Long shopId, String status, LocalDateTime startTime, LocalDateTime endTime,
            Integer limit, Integer offset);

    @Select("""
            <script>
            SELECT COUNT(*) FROM orders WHERE shop_id = #{shopId}
            <if test='status != null and status != ""'> AND status = #{status} </if>
            <if test='startTime != null'> AND created_at &gt;= #{startTime} </if>
            <if test='endTime != null'> AND created_at &lt;= #{endTime} </if>
            </script>
            """)
    long countByShopId(
            Long shopId, String status, LocalDateTime startTime, LocalDateTime endTime);

    @Select("SELECT * FROM orders WHERE id = #{id}")
    Optional<Order> findById(Long id);

    @Select("SELECT * FROM orders WHERE id = #{id} FOR UPDATE")
    Optional<Order> findByIdForUpdate(Long id);

    @Select("SELECT * FROM order_items WHERE order_id = #{orderId} ORDER BY id ASC")
    List<OrderItem> findItemsByOrderId(Long orderId);

    @Update("""
            UPDATE orders SET status = 'CANCELLED'
            WHERE id = #{orderId} AND status = 'CREATED' AND payment_status = 'UNPAID'
            """)
    int markCancelledIfAllowed(Long orderId);

    @Update("""
            UPDATE orders SET status = #{to}
            WHERE id = #{id} AND status = #{from}
            """)
    int transitionStatus(Long id, String from, String to);

    @Update("""
            UPDATE orders SET payment_status = 'PAID', paid_at = #{paidAt}
            WHERE id = #{id} AND user_id = #{userId} AND status = 'CREATED'
              AND payment_status = 'UNPAID' AND payment_deadline > #{paidAt}
            """)
    int markPaid(Long id, Long userId, LocalDateTime paidAt);

    @Select("SELECT * FROM orders WHERE status = 'CREATED' AND payment_status = 'UNPAID' AND payment_deadline <= #{now}")
    List<Order> findExpiredUnpaid(LocalDateTime now);

    @Update("""
            UPDATE orders SET status = 'CANCELLED'
            WHERE id = #{id} AND status = 'CREATED' AND payment_status = 'UNPAID' AND payment_deadline <= #{now}
            """)
    int cancelExpired(Long id, LocalDateTime now);

    @Update("""
            UPDATE orders SET rider_id = #{riderId}, status = 'DELIVERING'
            WHERE id = #{orderId} AND status = 'ACCEPTED' AND payment_status = 'PAID' AND rider_id IS NULL
            """)
    int claimForDelivery(Long orderId, Long riderId);

    @Select("SELECT * FROM orders WHERE status = 'ACCEPTED' AND payment_status = 'PAID' AND rider_id IS NULL ORDER BY created_at ASC, id ASC")
    List<Order> findReadyForDelivery();

    @Select("SELECT * FROM orders WHERE rider_id = #{riderId} ORDER BY created_at DESC, id DESC")
    List<Order> findByRiderId(Long riderId);

    @Update("UPDATE orders SET status = 'DELIVERED' WHERE id = #{orderId} AND rider_id = #{riderId} AND status = 'DELIVERING'")
    int markDelivered(Long orderId, Long riderId);

    @Select("SELECT * FROM orders ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Order> findPageForAdmin(int limit, int offset);
    @Select("SELECT COUNT(*) FROM orders") long countAll();
    @Update("UPDATE orders SET status = #{status} WHERE id = #{id}") int setStatusForAdmin(Long id, String status);
    @Select("SELECT COUNT(*) FROM orders WHERE user_id=#{userId} AND status NOT IN ('COMPLETED','CANCELLED')")
    long countActiveByUser(Long userId);
}
