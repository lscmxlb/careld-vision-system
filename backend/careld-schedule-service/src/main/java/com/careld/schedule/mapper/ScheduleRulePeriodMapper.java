package com.careld.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.schedule.entity.ScheduleRulePeriod;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScheduleRulePeriodMapper extends BaseMapper<ScheduleRulePeriod> {

    @Delete("DELETE FROM schedule_rule_period WHERE rule_id = #{ruleId}")
    int deleteByRuleId(@Param("ruleId") Long ruleId);
}
