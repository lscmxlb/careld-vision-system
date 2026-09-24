package com.careld.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.user.entity.MedicalStaff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MedicalStaffMapper extends BaseMapper<MedicalStaff> {

    @Select("SELECT * FROM medical_staff WHERE store_id = #{storeId} AND phone = #{phone} AND deleted_at IS NULL LIMIT 1")
    MedicalStaff selectByStoreAndPhone(@Param("storeId") Long storeId, @Param("phone") String phone);

    /** 手机号码查询：按手机号模糊匹配全部医务人员记录 */
    @Select("SELECT * FROM medical_staff WHERE phone LIKE CONCAT('%', #{phone}, '%') AND deleted_at IS NULL ORDER BY id")
    java.util.List<MedicalStaff> selectListByPhoneLike(@Param("phone") String phone);
}
