package com.careld.schedule.service;

import com.careld.schedule.entity.AppointmentConfig;
import com.careld.schedule.entity.ScheduleRule;
import com.careld.schedule.entity.ScheduleSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 排班规则 / 可约时段 / 预约规则配置
 */
public interface ScheduleRuleService {

    /** 创建排班规则并物化生成时段，返回规则ID */
    Long createRule(ScheduleRule rule);

    /** 修改规则（区间内已有时段存在预约时限制），返回规则ID */
    void updateRule(Long id, ScheduleRule rule);

    /** 删除规则（仅区间内无预约时可删） */
    void deleteRule(Long id);

    /** 规则列表（含例外日） */
    List<ScheduleRule> listRules(Long storeId);

    /** 查询某医院某日期的可约时段 */
    List<ScheduleSlot> getSlots(Long storeId, LocalDate date);

    /** 查询某医院在日期范围内的有排班日期（去重） */
    List<LocalDate> getAvailableDates(Long storeId, LocalDate startDate, LocalDate endDate);

    /** 按天聚合开放时段名额（日历展示：date/booked/available） */
    List<Map<String, Object>> getSlotDailySummary(Long storeId, LocalDate startDate, LocalDate endDate);

    /** 读取预约规则配置（无配置时返回默认值） */
    AppointmentConfig getConfig(Long storeId);

    /** 保存预约规则配置 */
    void saveConfig(AppointmentConfig config);
}
