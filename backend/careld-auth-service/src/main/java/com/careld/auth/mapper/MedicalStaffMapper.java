package com.careld.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.auth.entity.MedicalStaff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface MedicalStaffMapper extends BaseMapper<MedicalStaff> {

    /** 按手机号（登录账号）查询启用状态的医务人员；同号跨院时取最早创建的一条 */
    @Select("SELECT * FROM medical_staff WHERE phone = #{phone} AND status = 1 AND deleted_at IS NULL ORDER BY id LIMIT 1")
    MedicalStaff selectEnabledByPhone(String phone);

    /** 记录登录时间，供医院活跃统计使用 */
    @Update("UPDATE medical_staff SET last_login_time = #{time} WHERE id = #{id}")
    int updateLastLoginTime(@Param("id") Long id, @Param("time") LocalDateTime time);
}
