package com.careld.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.store.entity.TvDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * TV 设备 Mapper
 */
@Mapper
public interface TvDeviceMapper extends BaseMapper<TvDevice> {

    @Select("SELECT * FROM store_tv_device WHERE device_code = #{deviceCode} LIMIT 1")
    TvDevice selectByDeviceCode(String deviceCode);

    @Select("SELECT COUNT(*) FROM store_tv_device WHERE device_type_id = #{typeId} AND deleted_at IS NULL")
    int countByTypeId(Long typeId);
}
