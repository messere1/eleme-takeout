package cn.edu.tju.takeout.shop;

import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShopMapper {
    @Insert("""
            INSERT INTO shops(merchant_id, shop_name, notice, status)
            VALUES(#{merchantId}, #{shopName}, #{notice}, #{status})
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
}
