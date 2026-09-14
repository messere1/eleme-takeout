package cn.edu.tju.takeout.shop;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShopMapper {
    @Insert("""
            INSERT INTO shops(merchant_id, shop_name, notice, status, shop_address, image_url, cover_image_url,opening_time,closing_time)
            VALUES(#{merchantId}, #{shopName}, #{notice}, #{status}, #{shopAddress}, #{imageUrl}, #{coverImageUrl}, #{openingTime}, #{closingTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Shop shop);

    @Select("SELECT * FROM shops WHERE id = #{id}")
    Optional<Shop> findById(Long id);

    @Select("SELECT * FROM shops WHERE merchant_id = #{merchantId}")
    Optional<Shop> findByMerchantId(Long merchantId);

    @Update("UPDATE shops SET status = #{status} WHERE id = #{id}")
    void updateStatus(Shop shop);

    @Update("UPDATE shops SET shop_name = #{shopName}, notice = #{notice} WHERE id = #{id}")
    void updateInfo(Shop shop);

    @Select("SELECT * FROM shops WHERE status = 'OPEN' ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Shop> findPage(int limit, int offset);

    @Select("SELECT COUNT(*) FROM shops WHERE status = 'OPEN'")
    long countAll();

    @Select("""
        SELECT s.*
        FROM shops s
        JOIN merchants m ON m.id = s.merchant_id
        WHERE s.status = 'OPEN'
          AND m.enabled = TRUE
          AND m.business_scope = #{businessScope}
        ORDER BY s.id DESC
        LIMIT #{limit} OFFSET #{offset}
    """)
    List<Shop> findPageByBusinessScope(
            @Param("businessScope") String businessScope,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
        SELECT COUNT(*)
        FROM shops s
        JOIN merchants m ON m.id = s.merchant_id
        WHERE s.status = 'OPEN'
        AND m.enabled = TRUE
        AND m.business_scope = #{businessScope}
    """)
    long countByBusinessScope(@Param("businessScope") String businessScope);

    @Select("""
        SELECT s.* FROM shops s
        JOIN merchants m ON m.id=s.merchant_id
        JOIN shop_business_categories sbc ON sbc.shop_id=s.id
        WHERE s.status='OPEN' AND m.enabled=TRUE AND sbc.business_category_id=#{categoryId}
        ORDER BY s.id DESC LIMIT #{limit} OFFSET #{offset}
    """) List<Shop> findPageByBusinessCategoryId(@Param("categoryId") Long categoryId,@Param("limit") int limit,@Param("offset") int offset);
    @Select("""
        SELECT COUNT(*) FROM shops s
        JOIN merchants m ON m.id=s.merchant_id
        JOIN shop_business_categories sbc ON sbc.shop_id=s.id
        WHERE s.status='OPEN' AND m.enabled=TRUE AND sbc.business_category_id=#{categoryId}
    """) long countByBusinessCategoryId(Long categoryId);

    @Update("UPDATE shops SET image_url = #{url} WHERE id = #{id}") int updateImage(Long id, String url);
    @Update("UPDATE shops SET cover_image_url = #{url} WHERE id = #{id}") int updateCover(Long id, String url);
    @Update("UPDATE shops SET shop_name=#{name}, shop_address=#{address} WHERE id=#{id}") int updateProfile(Long id,String name,String address);
    @Update("UPDATE shops SET status='CLOSED' WHERE merchant_id=#{merchantId}") int closeByMerchantId(Long merchantId);
    @Update("UPDATE shops SET opening_time = #{openingTime}, closing_time = #{closingTime} WHERE id = #{id}") int updateBusinessHours(Long id,LocalTime openingTime,LocalTime closingTime);
    
}
