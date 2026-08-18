package com.careld.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.user.entity.Agent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {

    @Select("SELECT agent_id, COUNT(*) as cnt FROM store_info WHERE deleted_at IS NULL GROUP BY agent_id")
    List<Map<String, Object>> countStoresGroupByAgent();
}
