package com.careld.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 排班规则时段：每条规则在"周一~五 / 周六日"下可配置多个不重复时段
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_rule_period")
public class ScheduleRulePeriod extends BaseEntity {
    private Long ruleId;
    /** 日类型:1周一~五 2周六日 */
    private Integer dayType;
    private LocalTime startTime;
    private LocalTime endTime;
    /** 每小时接待上限 */
    private Integer capacity;
}
