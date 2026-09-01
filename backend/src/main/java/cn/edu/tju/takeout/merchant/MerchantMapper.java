package cn.edu.tju.takeout.merchant;

import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MerchantMapper {
    @Select("SELECT * FROM merchants WHERE merchant_name = #{name}")
    Optional<Merchant> findByName(String name);
    @Select("SELECT * FROM merchants WHERE phone = #{phone}")
    Optional<Merchant> findByPhone(String phone);
    @Insert("""
            INSERT INTO merchants(merchant_name, phone, password_hash, business_scope, created_at)
            VALUES(#{merchantName}, #{phone}, #{passwordHash}, #{businessScope}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Merchant merchant);
}
