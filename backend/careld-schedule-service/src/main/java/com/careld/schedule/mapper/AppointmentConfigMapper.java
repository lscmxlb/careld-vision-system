package com.careld.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.schedule.entity.AppointmentConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppointmentConfigMapper extends BaseMapper<AppointmentConfig> {

    @Select("SELECT * FROM appointment_config WHERE store_id = #{storeId} LIMIT 1")
    AppointmentConfig selectByStoreId(@Param("storeId") Long storeId);
}
