package com.careld.auth.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 短信验证码存取
 */
@Mapper
public interface SmsCodeMapper {

    @Insert("INSERT INTO sms_code (phone, code, scene, expire_at, used, created_at) "
            + "VALUES (#{phone}, #{code}, 1, #{expireAt}, 0, NOW())")
    int insertCode(@Param("phone") String phone, @Param("code") String code, @Param("expireAt") LocalDateTime expireAt);

    @Select("SELECT code FROM sms_code WHERE phone = #{phone} AND used = 0 AND expire_at > NOW() "
            + "ORDER BY id DESC LIMIT 1")
    String selectLatestValid(@Param("phone") String phone);

    @Update("UPDATE sms_code SET used = 1 WHERE phone = #{phone} AND code = #{code} AND used = 0")
    int markUsed(@Param("phone") String phone, @Param("code") String code);

    @Update("UPDATE sms_code SET used = 1 WHERE phone = #{phone} AND used = 0")
    int invalidateAll(@Param("phone") String phone);
}
