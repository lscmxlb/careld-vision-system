package com.careld.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.schedule.entity.ScheduleRuleException;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleRuleExceptionMapper extends BaseMapper<ScheduleRuleException> {

    @Select("SELECT * FROM schedule_rule_exception WHERE rule_id = #{ruleId}")
    List<ScheduleRuleException> selectByRuleId(@Param("ruleId") Long ruleId);

    @Delete("DELETE FROM schedule_rule_exception WHERE rule_id = #{ruleId}")
    int deleteByRuleId(@Param("ruleId") Long ruleId);
}
