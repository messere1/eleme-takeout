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
            INSERT INTO orders(order_no, user_id, shop_id, total_amount, status, address, created_at)
            VALUES(#{orderNo}, #{userId}, #{shopId}, #{totalAmount}, #{status}, #{address}, #{createdAt})
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

    @Select("SELECT * FROM order_items WHERE order_id = #{orderId} ORDER BY id ASC")
    List<OrderItem> findItemsByOrderId(Long orderId);

    @Update("""
            UPDATE orders SET status = 'CANCELLED'
            WHERE id = #{orderId} AND status = 'CREATED'
            """)
    int markCancelledIfAllowed(Long orderId);

    @Update("""
            UPDATE orders SET status = #{to}
            WHERE id = #{id} AND status = #{from}
            """)
    int transitionStatus(Long id, String from, String to);
}
