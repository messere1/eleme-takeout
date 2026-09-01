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

    @Select("SELECT * FROM users WHERE username = #{account} OR phone = #{account}")
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
}
