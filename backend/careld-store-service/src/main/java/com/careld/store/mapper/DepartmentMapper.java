package com.careld.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.store.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 科室Mapper
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 根据门店ID查询科室列表
     */
    @Select("SELECT * FROM store_department WHERE store_id = #{storeId} AND status = 1 ORDER BY sort_order, id")
    List<Department> selectByStoreId(@Param("storeId") Long storeId);
}
