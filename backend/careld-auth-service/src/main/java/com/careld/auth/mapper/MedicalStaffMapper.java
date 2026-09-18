package com.careld.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.auth.entity.MedicalStaff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MedicalStaffMapper extends BaseMapper<MedicalStaff> {

    /** 按手机号（登录账号）查询启用状态的医务人员；同号跨院时取最早创建的一条 */
    @Select("SELECT * FROM medical_staff WHERE phone = #{phone} AND status = 1 AND deleted_at IS NULL ORDER BY id LIMIT 1")
    MedicalStaff selectEnabledByPhone(String phone);
}
