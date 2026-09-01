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
            INSERT INTO orders(order_no, user_id, shop_id, total_amount, status, created_at)
            VALUES(#{orderNo}, #{userId}, #{shopId}, #{totalAmount}, #{status}, #{createdAt})
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

    @Select("SELECT * FROM orders WHERE id = #{id}")
    Optional<Order> findById(Long id);

    @Select("SELECT * FROM order_items WHERE order_id = #{orderId} ORDER BY id ASC")
    List<OrderItem> findItemsByOrderId(Long orderId);

    @Update("""
            UPDATE orders SET status = 'CANCELLED'
            WHERE id = #{orderId} AND status IN ('PENDING', 'PAID')
            """)
    int markCancelledIfAllowed(Long orderId);
}
