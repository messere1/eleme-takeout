package cn.edu.tju.takeout.cart;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CartMapper {
    @Select("""
            SELECT * FROM cart_items
            WHERE user_id = #{userId} AND product_id = #{productId}
            """)
    Optional<CartItem> findByUserAndProduct(Long userId, Long productId);

    @Insert("""
            INSERT INTO cart_items(user_id, product_id, quantity)
            VALUES(#{userId}, #{productId}, #{quantity})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CartItem item);

    @Update("UPDATE cart_items SET quantity = #{quantity} WHERE id = #{id}")
    void updateQuantity(CartItem item);

    @Select("""
            SELECT ci.id, p.shop_id, ci.product_id, p.name AS product_name, p.price,
                   ci.quantity, p.stock, p.status
            FROM cart_items ci
            JOIN products p ON p.id = ci.product_id
            WHERE ci.user_id = #{userId}
            ORDER BY ci.id ASC
            """)
    List<CartLine> findDetailsByUserId(Long userId);

    @Select("SELECT * FROM cart_items WHERE id = #{id} AND user_id = #{userId}")
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    @Delete("DELETE FROM cart_items WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(Long id, Long userId);

    @Delete("DELETE FROM cart_items WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Delete("""
            DELETE FROM cart_items
            WHERE user_id = #{userId}
              AND product_id IN (SELECT id FROM products WHERE shop_id = #{shopId})
            """)
    int deleteByUserIdAndShopId(Long userId, Long shopId);

    @Select("""
            SELECT ci.id AS cart_item_id, p.shop_id, p.id AS product_id,
                   p.name AS product_name, p.price, ci.quantity, p.stock, p.status
            FROM cart_items ci
            JOIN products p ON p.id = ci.product_id
            WHERE ci.user_id = #{userId} AND p.deleted = FALSE
            ORDER BY ci.id ASC
            """)
    List<CartCheckoutLine> findCheckoutLinesByUserId(Long userId);

    @Select("SELECT * FROM cart_delivery_info WHERE user_id = #{userId} AND shop_id = #{shopId}")
    Optional<CartDeliveryInfo> findDeliveryInfo(Long userId, Long shopId);

    @Insert("INSERT INTO cart_delivery_info(user_id,shop_id,recipient_name,recipient_phone,delivery_address) VALUES(#{userId},#{shopId},#{recipientName},#{recipientPhone},#{deliveryAddress})")
    void insertDeliveryInfo(CartDeliveryInfo info);
    @Update("UPDATE cart_delivery_info SET recipient_name=#{recipientName},recipient_phone=#{recipientPhone},delivery_address=#{deliveryAddress} WHERE user_id=#{userId} AND shop_id=#{shopId}")
    int updateDeliveryInfo(CartDeliveryInfo info);
}
