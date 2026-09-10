package cn.edu.tju.takeout.merchant;

import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MerchantMapper {
    @Select("SELECT * FROM merchants WHERE enabled = TRUE AND merchant_name = #{name}")
    Optional<Merchant> findByName(String name);
    @Select("SELECT * FROM merchants WHERE enabled = TRUE AND phone = #{phone}")
    Optional<Merchant> findByPhone(String phone);

    @Select("SELECT * FROM merchants WHERE id = #{id}")
    Optional<Merchant> findById(Long id);
    @Insert("""
            INSERT INTO merchants(merchant_name, phone, password_hash, business_scope, created_at)
            VALUES(#{merchantName}, #{phone}, #{passwordHash}, #{businessScope}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Merchant merchant);

    @Select("SELECT * FROM merchants ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    java.util.List<Merchant> findPage(int limit, int offset);
    @Select("SELECT COUNT(*) FROM merchants") long countAll();
    @Update("UPDATE merchants SET enabled = #{enabled} WHERE id = #{id}") int setEnabled(Long id, boolean enabled);
    @Update("UPDATE merchants SET business_scope=#{scope} WHERE id=#{id} AND enabled=TRUE") int updateScope(Long id,String scope);
    @Update("UPDATE merchants SET enabled=FALSE, phone=CONCAT('DELETED-',id), merchant_name=CONCAT('deleted-',id) WHERE id=#{id} AND enabled=TRUE") int softDelete(Long id);
}
