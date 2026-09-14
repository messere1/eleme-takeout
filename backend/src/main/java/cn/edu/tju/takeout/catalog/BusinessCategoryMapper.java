package cn.edu.tju.takeout.catalog;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessCategoryMapper {
    @Select("""
        SELECT * FROM business_categories
        WHERE enabled = TRUE AND default_category = TRUE
        ORDER BY id
    """)
    List<BusinessCategory> findEnabled();
    @Select("SELECT * FROM business_categories WHERE name = #{name}") Optional<BusinessCategory> findByName(String name);
    @Insert("INSERT INTO business_categories(name, default_category, enabled) VALUES(#{name}, FALSE, TRUE)")
    @Options(useGeneratedKeys = true, keyProperty = "id") void insert(MutableCategory category);
    @Insert("INSERT INTO business_categories(name, default_category, enabled) VALUES(#{name}, TRUE, TRUE)")
    void insertDefault(String name);
    @Select("SELECT COUNT(*) FROM business_categories WHERE id = #{id} AND enabled = TRUE") int countEnabledById(Long id);
    @Select("""
        SELECT bc.* FROM business_categories bc
        JOIN shop_business_categories sbc ON sbc.business_category_id = bc.id
        WHERE sbc.shop_id = #{shopId} AND bc.enabled = TRUE ORDER BY bc.id
    """) List<BusinessCategory> findByShopId(Long shopId);
    @Delete("DELETE FROM shop_business_categories WHERE shop_id = #{shopId}") void deleteByShopId(Long shopId);
    @Insert("INSERT INTO shop_business_categories(shop_id,business_category_id) VALUES(#{shopId},#{categoryId})")
    void linkShop(@Param("shopId") Long shopId,@Param("categoryId") Long categoryId);
    class MutableCategory {
        private Long id; private final String name;
        public MutableCategory(String name) { this.name = name; }
        public Long getId() { return id; } public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
    }
}
