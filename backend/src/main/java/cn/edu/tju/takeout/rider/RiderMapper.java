package cn.edu.tju.takeout.rider;

import java.util.Optional;
import org.apache.ibatis.annotations.*;

@Mapper
public interface RiderMapper {
    @Select("SELECT * FROM riders WHERE enabled = TRUE AND (rider_name = #{account} OR phone = #{account})")
    Optional<Rider> findByAccount(String account);
    @Select("SELECT COUNT(*) FROM riders") long countAll();
    @Insert("INSERT INTO riders(rider_name, phone, password_hash, enabled) VALUES(#{name},#{phone},#{hash},TRUE)")
    void insert(String name, String phone, String hash);
}
