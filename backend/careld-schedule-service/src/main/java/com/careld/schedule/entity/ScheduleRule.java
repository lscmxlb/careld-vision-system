package com.careld.schedule.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 排班规则（自然日区间 + 周类型）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_rule")
public class ScheduleRule extends BaseEntity {
    private Long storeId;
    private LocalDate startDate;
    private LocalDate endDate;
    // 以下 6 个旧列为多时段明细的冗余字段：未配置的日类型必须写空，
    // 故更新策略用 ALWAYS（默认 NOT_NULL 会跳过 null，导致保留旧值）
    /** 周一~五接待起始时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalTime weekdayStartTime;
    /** 周一~五接待结束时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalTime weekdayEndTime;
    /** 周一~五每小时接待上限 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer weekdayCapacity;
    /** 周六日接待起始时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalTime weekendStartTime;
    /** 周六日接待结束时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalTime weekendEndTime;
    /** 周六日每小时接待上限 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer weekendCapacity;
    private Integer status;

    /** 多时段列表（不映射数据库列，保存/查询时与 schedule_rule_period 联动） */
    @TableField(exist = false)
    private List<ScheduleRulePeriod> periods;

    /** 例外日列表（带备注，不映射数据库列） */
    @TableField(exist = false)
    private List<ScheduleRuleException> exceptions;

    /** 例外日列表（不映射数据库列，兼容旧字段） */
    @TableField(exist = false)
    private List<LocalDate> exceptionDates;
}
