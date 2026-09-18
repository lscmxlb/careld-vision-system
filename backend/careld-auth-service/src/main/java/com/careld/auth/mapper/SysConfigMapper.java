package com.careld.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.auth.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置存取
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    @Select("SELECT config_value FROM sys_config WHERE config_key = #{key}")
    String selectValue(@Param("key") String key);
}
