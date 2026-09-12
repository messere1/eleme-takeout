package cn.edu.tju.takeout.shop;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShopMapper {
    @Insert("""
            INSERT INTO shops(merchant_id, shop_name, notice, status, shop_address, image_url, cover_image_url)
            VALUES(#{merchantId}, #{shopName}, #{notice}, #{status}, #{shopAddress}, #{imageUrl}, #{coverImageUrl})
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

    @Update("UPDATE shops SET image_url = #{url} WHERE id = #{id}") int updateImage(Long id, String url);
    @Update("UPDATE shops SET cover_image_url = #{url} WHERE id = #{id}") int updateCover(Long id, String url);
    @Update("UPDATE shops SET shop_name=#{name}, shop_address=#{address} WHERE id=#{id}") int updateProfile(Long id,String name,String address);
    @Update("UPDATE shops SET status='CLOSED' WHERE merchant_id=#{merchantId}") int closeByMerchantId(Long merchantId);
}
