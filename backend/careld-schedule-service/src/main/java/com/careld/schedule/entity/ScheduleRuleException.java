package com.careld.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 排班规则例外日（该日不适用区间规则，不生成时段）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_rule_exception")
public class ScheduleRuleException extends BaseEntity {
    private Long ruleId;
    private LocalDate exceptionDate;
    /** 例外说明（该批例外日的原因） */
    private String remark;
}
