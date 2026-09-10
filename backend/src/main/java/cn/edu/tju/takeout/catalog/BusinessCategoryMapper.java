package cn.edu.tju.takeout.catalog;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BusinessCategoryMapper {
    @Select("SELECT * FROM business_categories WHERE enabled = TRUE ORDER BY default_category DESC, id")
    List<BusinessCategory> findEnabled();
    @Select("SELECT * FROM business_categories WHERE name = #{name}") Optional<BusinessCategory> findByName(String name);
    @Insert("INSERT INTO business_categories(name, default_category, enabled) VALUES(#{name}, FALSE, TRUE)")
    @Options(useGeneratedKeys = true, keyProperty = "id") void insert(MutableCategory category);
    @Insert("INSERT INTO business_categories(name, default_category, enabled) VALUES(#{name}, TRUE, TRUE)")
    void insertDefault(String name);
    class MutableCategory {
        private Long id; private final String name;
        public MutableCategory(String name) { this.name = name; }
        public Long getId() { return id; } public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
    }
}
