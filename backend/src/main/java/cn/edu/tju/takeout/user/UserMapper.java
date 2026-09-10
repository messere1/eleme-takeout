package cn.edu.tju.takeout.user;

import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(String username);

    @Select("SELECT * FROM users WHERE phone = #{phone}")
    Optional<User> findByPhone(String phone);

    @Select("SELECT * FROM users WHERE enabled = TRUE AND (username = #{account} OR phone = #{account})")
    Optional<User> findByAccount(String account);

    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);

    @Insert("""
            INSERT INTO users(username, phone, password_hash, nickname, created_at)
            VALUES(#{username}, #{phone}, #{passwordHash}, #{nickname}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    @Update("""
            UPDATE users SET nickname = #{nickname}, phone = #{phone}, address = #{address}
            WHERE id = #{id}
            """)
    void updateProfile(User user);

    @Update("UPDATE users SET avatar_url = #{avatarUrl} WHERE id = #{id} AND enabled = TRUE")
    int updateAvatar(Long id, String avatarUrl);

    @Update("""
            UPDATE users SET enabled = FALSE, deleted_at = CURRENT_TIMESTAMP,
              phone = CONCAT('DELETED-', id), username = CONCAT('deleted-', id), nickname = '已注销用户', address = NULL
            WHERE id = #{id} AND enabled = TRUE
            """)
    int softDelete(Long id);

    @Select("SELECT * FROM users ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    java.util.List<User> findPage(int limit, int offset);

    @Select("SELECT COUNT(*) FROM users")
    long countAll();

    @Update("UPDATE users SET enabled = #{enabled} WHERE id = #{id}")
    int setEnabled(Long id, boolean enabled);
    @Update("UPDATE users SET address = #{address} WHERE id = #{id} AND enabled = TRUE")
    int updateAddress(Long id, String address);
}
