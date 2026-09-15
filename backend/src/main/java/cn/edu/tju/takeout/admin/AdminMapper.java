package cn.edu.tju.takeout.admin;

import java.util.Optional;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminMapper {
    @Select("SELECT * FROM administrators WHERE username = #{account} AND enabled = TRUE")
    Optional<Admin> findByAccount(@Param("account") String account);
    @Select("SELECT * FROM administrators WHERE id = #{id}") Optional<Admin> findById(Long id);
    @Select("SELECT COUNT(*) FROM administrators") long countAll();
    @Select("SELECT COUNT(*) FROM administrators WHERE enabled = TRUE") long countEnabled();
    @Update("UPDATE administrators SET enabled = #{enabled} WHERE id = #{id}")
    int setEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);
    @Insert("INSERT INTO administrators(username, password_hash, enabled) VALUES(#{username}, #{hash}, TRUE)")
    void insert(@Param("username") String username, @Param("hash") String hash);
    @Insert("INSERT INTO admin_audit_logs(admin_id, action, target_type, target_id, created_at) VALUES(#{adminId},#{action},#{type},#{targetId},CURRENT_TIMESTAMP)")
    void audit(
            @Param("adminId") Long adminId,
            @Param("action") String action,
            @Param("type") String type,
            @Param("targetId") Long targetId);
}
