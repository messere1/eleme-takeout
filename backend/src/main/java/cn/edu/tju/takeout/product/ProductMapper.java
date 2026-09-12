package cn.edu.tju.takeout.product;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper {
    @Select("SELECT COUNT(*) FROM products WHERE category_id = #{categoryId} AND deleted = FALSE")
    int countByCategoryId(Long categoryId);

    @Select("SELECT * FROM products WHERE id = #{id} AND deleted = FALSE")
    Optional<Product> findById(Long id);

    @Select("""
            SELECT * FROM products
            WHERE shop_id = #{shopId} AND category_id = #{categoryId}
              AND status = 'ON_SALE' AND deleted = FALSE
              AND EXISTS (SELECT 1 FROM shops s WHERE s.id = products.shop_id AND s.status = 'OPEN')
            ORDER BY id ASC
            """)
    List<Product> findVisibleByShopAndCategory(Long shopId, Long categoryId);

    @Insert("""
            INSERT INTO products(shop_id, category_id, name, description, price, stock, status, deleted)
            VALUES(#{shopId}, #{categoryId}, #{name}, #{description}, #{price}, #{stock}, #{status}, FALSE)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Product product);

    @Update("""
            UPDATE products SET category_id = #{categoryId}, name = #{name},
              description = #{description}, price = #{price}, stock = #{stock}
            WHERE id = #{id} AND deleted = FALSE
            """)
    void update(Product product);

    @Update("UPDATE products SET status = #{status} WHERE id = #{id} AND deleted = FALSE")
    void updateStatus(Product product);

    @Update("UPDATE products SET deleted = TRUE, status = 'OFF_SALE' WHERE id = #{id}")
    void logicalDelete(Long id);

    @Update("UPDATE products SET stock = #{stock} WHERE id = #{id} AND deleted = FALSE")
    void updateStock(Product product);

    @Update("UPDATE products SET price = #{price} WHERE id = #{id} AND deleted = FALSE")
    void updatePrice(Product product);

    @Update("""
            UPDATE products SET stock = stock - #{quantity}
            WHERE id = #{productId} AND deleted = FALSE AND stock >= #{quantity}
            """)
    int decreaseStockIfAvailable(Long productId, Integer quantity);

    @Update("UPDATE products SET stock = stock + #{quantity} WHERE id = #{productId}")
    int increaseStock(Long productId, Integer quantity);

    //用于分页查询
    @Select("""
        SELECT * FROM products WHERE category_id = #{categoryId} AND status = 'ON_SALE'
        AND deleted = FALSE
        AND EXISTS (SELECT 1 FROM shops s WHERE s.id = products.shop_id AND s.status = 'OPEN')
        ORDER BY id ASC LIMIT #{limit} OFFSET #{offset}
        """)
    List<Product> findVisiblePageByCategoryId(
                Long categoryId,
                int limit,
                int offset
        );

    @Select("""
                SELECT COUNT(*)
                FROM products
                WHERE category_id = #{categoryId}
                AND status = 'ON_SALE'
                AND deleted = FALSE
                AND EXISTS (SELECT 1 FROM shops s WHERE s.id = products.shop_id AND s.status = 'OPEN')
        """)
    long countVisibleByCategoryId(Long categoryId);

    //用于查看商品详情
    @Select("""
        SELECT *
        FROM products
        WHERE id=#{productId}
        AND status='ON_SALE'
        AND deleted=FALSE
        AND EXISTS (SELECT 1 FROM shops s WHERE s.id = products.shop_id AND s.status = 'OPEN')
        """)
    Optional<Product> findVisibleById(Long productId);

    @Select("""
            SELECT *
            FROM products
            WHERE shop_id = #{shopId}
              AND deleted = FALSE
            ORDER BY category_id ASC, id ASC
            """)
    List<Product> findAllByShopId(Long shopId);

    @Update("UPDATE products SET image_url = #{url} WHERE id = #{id} AND deleted = FALSE")
    int updateImage(Long id, String url);

    @Select("SELECT * FROM products WHERE deleted = FALSE ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<Product> findPageForAdmin(int limit, int offset);
    @Select("SELECT COUNT(*) FROM products WHERE deleted = FALSE") long countForAdmin();
    @Update("UPDATE products SET status = #{status} WHERE id = #{id} AND deleted = FALSE")
    int setStatusForAdmin(Long id, String status);
}
