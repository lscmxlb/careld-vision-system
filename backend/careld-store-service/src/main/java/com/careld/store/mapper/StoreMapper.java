package com.careld.store.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.store.entity.Store;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StoreMapper extends BaseMapper<Store> {
    @Select("SELECT * FROM store_info WHERE store_code = #{storeCode} AND deleted_at IS NULL")
    Store selectByStoreCode(String storeCode);

    @Select("SELECT id, agent_name FROM agent WHERE deleted_at IS NULL")
    List<Map<String, Object>> selectAgentIdAndName();

    @Select("SELECT id, center_name FROM ops_center WHERE deleted_at IS NULL")
    List<Map<String, Object>> selectCenterIdAndName();

    @Select("SELECT id, center_id FROM agent WHERE deleted_at IS NULL")
    List<Map<String, Object>> selectAgentCenterMapping();
}
