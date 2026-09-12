package cn.edu.tju.takeout.admin;

import java.util.Optional;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminMapper {
    @Select("SELECT * FROM administrators WHERE username = #{account} AND enabled = TRUE")
    Optional<Admin> findByAccount(String account);
    @Select("SELECT * FROM administrators WHERE id = #{id}") Optional<Admin> findById(Long id);
    @Select("SELECT COUNT(*) FROM administrators") long countAll();
    @Insert("INSERT INTO administrators(username, password_hash, enabled) VALUES(#{username}, #{hash}, TRUE)")
    void insert(String username, String hash);
    @Insert("INSERT INTO admin_audit_logs(admin_id, action, target_type, target_id, created_at) VALUES(#{adminId},#{action},#{type},#{targetId},CURRENT_TIMESTAMP)")
    void audit(Long adminId, String action, String type, Long targetId);
}
