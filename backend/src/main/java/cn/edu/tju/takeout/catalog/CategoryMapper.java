package cn.edu.tju.takeout.catalog;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CategoryMapper {
    @Select("SELECT id, shop_id, name, sort_order AS sort FROM categories WHERE id = #{id}")
    Optional<Category> findById(Long id);

    @Select("""
            SELECT id, shop_id, name, sort_order AS sort FROM categories
            WHERE shop_id = #{shopId} AND name = #{name}
            """)
    Optional<Category> findByName(Long shopId, String name);

    @Select("""
            SELECT id, shop_id, name, sort_order AS sort FROM categories
            WHERE shop_id = #{shopId} AND sort_order = #{sort}
            """)
    Optional<Category> findBySort(Long shopId, Integer sort);

    @Select("""
            SELECT id, shop_id, name, sort_order AS sort FROM categories
            WHERE shop_id = #{shopId} ORDER BY sort_order ASC, id ASC
            """)
    List<Category> findAllByShopId(Long shopId);

    @Insert("INSERT INTO categories(shop_id, name, sort_order) VALUES(#{shopId}, #{name}, #{sort})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Category category);

    @Update("UPDATE categories SET name = #{name}, sort_order = #{sort} WHERE id = #{id}")
    void update(Category category);

    @Delete("DELETE FROM categories WHERE id = #{id}")
    void deleteById(Long id);
}
