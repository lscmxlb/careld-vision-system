package com.careld.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.schedule.entity.AppointmentConfig;
import com.careld.schedule.entity.ScheduleRule;
import com.careld.schedule.entity.ScheduleRuleException;
import com.careld.schedule.entity.ScheduleRulePeriod;
import com.careld.schedule.entity.ScheduleSlot;
import com.careld.schedule.mapper.AppointmentConfigMapper;
import com.careld.schedule.mapper.ScheduleRuleExceptionMapper;
import com.careld.schedule.mapper.ScheduleRuleMapper;
import com.careld.schedule.mapper.ScheduleRulePeriodMapper;
import com.careld.schedule.mapper.ScheduleSlotMapper;
import com.careld.schedule.service.ScheduleRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScheduleRuleServiceImpl implements ScheduleRuleService {

    private final ScheduleRuleMapper ruleMapper;
    private final ScheduleRuleExceptionMapper exceptionMapper;
    private final ScheduleRulePeriodMapper periodMapper;
    private final ScheduleSlotMapper slotMapper;
    private final AppointmentConfigMapper configMapper;

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    @Transactional
    public Long createRule(ScheduleRule rule) {
        validateRule(rule);
        // 同一日期只能设置一次：与既有启用规则区间不得重叠
        List<ScheduleRule> overlapping = ruleMapper.selectOverlapping(rule.getStoreId(), rule.getStartDate(), rule.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new BusinessException(4001, "所选日期区间与已有排班规则重叠，同一日期只能设置一次");
        }
        rule.setId(null);
        rule.setStatus(1);
        applyLegacyPeriodColumns(rule);
        ruleMapper.insert(rule);
        savePeriods(rule);
        saveExceptions(rule);
        materializeSlots(rule, List.of());
        return rule.getId();
    }

    @Override
    @Transactional
    public void updateRule(Long id, ScheduleRule rule) {
        ScheduleRule exist = ruleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "排班规则不存在");
        }
        // 更新以既有记录为准：请求体不带 storeId 时先补齐，再校验/查重叠
        rule.setId(id);
        rule.setStoreId(exist.getStoreId());
        validateRule(rule);
        // 与其他规则的区间重叠校验（排除自身）
        List<ScheduleRule> overlapping = ruleMapper.selectOverlapping(rule.getStoreId(), rule.getStartDate(), rule.getEndDate());
        if (overlapping.stream().anyMatch(r -> !r.getId().equals(id))) {
            throw new BusinessException(4001, "所选日期区间与已有排班规则重叠，同一日期只能设置一次");
        }
        // 影响面校验：扩容放行，仅当新规则不再覆盖已约时段或新容量小于已约人数时拒绝
        List<ScheduleSlot> bookedSlots = slotMapper.selectBookedInRange(exist.getStoreId(), exist.getStartDate(), exist.getEndDate());
        validateBookedCompatibility(rule, bookedSlots);
        applyLegacyPeriodColumns(rule);
        ruleMapper.updateById(rule);
        periodMapper.deleteByRuleId(id);
        savePeriods(rule);
        exceptionMapper.deleteByRuleId(id);
        saveExceptions(rule);
        // 重建时段：清除未占用的空闲时段，按新规则生成；已预约时段保留不重复生成
        slotMapper.deleteUnbookedInRange(exist.getStoreId(), exist.getStartDate(), exist.getEndDate());
        materializeSlots(rule, bookedSlots);
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        ScheduleRule exist = ruleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "排班规则不存在");
        }
        if (slotMapper.countBookedInRange(exist.getStoreId(), exist.getStartDate(), exist.getEndDate()) > 0) {
            throw new BusinessException(4003, "该规则区间内已存在预约，不可删除");
        }
        ruleMapper.deleteById(id);
        periodMapper.deleteByRuleId(id);
        exceptionMapper.deleteByRuleId(id);
        slotMapper.deleteUnbookedInRange(exist.getStoreId(), exist.getStartDate(), exist.getEndDate());
    }

    @Override
    public List<ScheduleRule> listRules(Long storeId) {
        List<ScheduleRule> rules = ruleMapper.selectByStore(storeId);
        for (ScheduleRule rule : rules) {
            List<ScheduleRuleException> exceptions = exceptionMapper.selectByRuleId(rule.getId());
            rule.setExceptions(exceptions);
            rule.setExceptionDates(exceptions.stream().map(ScheduleRuleException::getExceptionDate).toList());
            LambdaQueryWrapper<ScheduleRulePeriod> pw = new LambdaQueryWrapper<>();
            pw.eq(ScheduleRulePeriod::getRuleId, rule.getId());
            pw.orderByAsc(ScheduleRulePeriod::getStartTime);
            rule.setPeriods(periodMapper.selectList(pw));
        }
        return rules;
    }

    @Override
    public List<ScheduleSlot> getSlots(Long storeId, LocalDate date) {
        return slotMapper.selectByDate(storeId, date);
    }

    @Override
    public List<LocalDate> getAvailableDates(Long storeId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ScheduleSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleSlot::getStoreId, storeId);
        wrapper.ge(ScheduleSlot::getSlotDate, startDate);
        wrapper.le(ScheduleSlot::getSlotDate, endDate);
        wrapper.eq(ScheduleSlot::getStatus, 1);
        wrapper.orderByAsc(ScheduleSlot::getSlotDate);
        List<ScheduleSlot> slots = slotMapper.selectList(wrapper);
        Set<LocalDate> dates = new HashSet<>();
        slots.forEach(s -> dates.add(s.getSlotDate()));
        return new ArrayList<>(dates).stream().sorted().toList();
    }

    @Override
    public List<Map<String, Object>> getSlotDailySummary(Long storeId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = slotMapper.sumDailyByRange(storeId, startDate, endDate);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", String.valueOf(row.get("slotDate")));
            item.put("booked", row.get("booked") == null ? 0 : ((Number) row.get("booked")).intValue());
            item.put("available", row.get("available") == null ? 0 : ((Number) row.get("available")).intValue());
            result.add(item);
        }
        return result;
    }

    @Override
    public AppointmentConfig getConfig(Long storeId) {
        AppointmentConfig config = configMapper.selectByStoreId(storeId);
        if (config != null) {
            return config;
        }
        // 无配置时返回默认值（不落库，与建表默认一致）
        AppointmentConfig def = new AppointmentConfig();
        def.setStoreId(storeId);
        def.setParentCancelHours(24);
        def.setDoctorCancelHours(2);
        def.setNoShowBufferMinutes(15);
        def.setAutoCompleteHours(new java.math.BigDecimal("1.5"));
        def.setAutoNoShowHours(12);
        def.setDefaultShowStatuses("1,2,3,4,5");
        return def;
    }

    @Override
    @Transactional
    public void saveConfig(AppointmentConfig config) {
        validateDefaultShowStatuses(config.getDefaultShowStatuses());
        AppointmentConfig exist = configMapper.selectByStoreId(config.getStoreId());
        if (exist == null) {
            config.setId(null);
            configMapper.insert(config);
        } else {
            config.setId(exist.getId());
            configMapper.updateById(config);
        }
    }

    /** 预约记录默认显示选择：至少勾选一项，且仅允许 1-5 */
    private void validateDefaultShowStatuses(String statuses) {
        if (!org.springframework.util.StringUtils.hasText(statuses)) {
            throw new BusinessException(400, "预约记录默认显示至少勾选一项");
        }
        for (String s : statuses.split(",")) {
            String v = s.trim();
            if (!v.matches("[1-5]")) {
                throw new BusinessException(400, "预约记录默认显示选择不合法");
            }
        }
    }

    // ==================== 私有方法 ====================

    private void validateRule(ScheduleRule rule) {
        if (rule.getStoreId() == null || rule.getStartDate() == null || rule.getEndDate() == null) {
            throw new BusinessException(400, "医院与日期区间不能为空");
        }
        if (rule.getStartDate().isAfter(rule.getEndDate())) {
            throw new BusinessException(400, "区间起始日期不能晚于结束日期");
        }
        validatePeriods(rule);
    }

    /**
     * 校验多时段配置：每个日类型可留空（留空表示该类型不排班），但整体至少配置一条；
     * 已配置的时段需起止合法、整小时、每小时上限大于 0、同日类型内不重叠
     */
    private void validatePeriods(ScheduleRule rule) {
        List<ScheduleRulePeriod> periods = rule.getPeriods() == null ? List.of() : rule.getPeriods().stream()
                .filter(p -> p.getDayType() != null)
                .toList();
        if (periods.isEmpty()) {
            throw new BusinessException(400, "请至少配置一个接待时段（工作日或周六日均可）");
        }
        for (String label : new String[]{"周一~五", "周六日"}) {
            int dayType = label.startsWith("周一") ? 1 : 2;
            List<ScheduleRulePeriod> group = periods.stream()
                    .filter(p -> p.getDayType() == dayType)
                    .sorted(java.util.Comparator.comparing(ScheduleRulePeriod::getStartTime,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .toList();
            if (group.isEmpty()) {
                continue;
            }
            LocalTime prevEnd = null;
            for (ScheduleRulePeriod p : group) {
                if (p.getStartTime() == null || p.getEndTime() == null) {
                    throw new BusinessException(400, label + "接待时段起止时间不能为空");
                }
                if (!p.getEndTime().isAfter(p.getStartTime())) {
                    throw new BusinessException(400, label + "时段 " + HM.format(p.getStartTime()) + " 结束时间必须晚于起始时间");
                }
                long minutes = java.time.Duration.between(p.getStartTime(), p.getEndTime()).toMinutes();
                if (minutes % 60 != 0) {
                    throw new BusinessException(400, label + "时段 " + HM.format(p.getStartTime()) + "~" + HM.format(p.getEndTime()) + " 必须为整小时（如 8:30~11:30）");
                }
                if (p.getCapacity() == null || p.getCapacity() < 1) {
                    throw new BusinessException(400, label + "时段 " + HM.format(p.getStartTime()) + " 每小时接待人数上限必须大于 0");
                }
                if (prevEnd != null && p.getStartTime().isBefore(prevEnd)) {
                    throw new BusinessException(400, label + "时段 " + HM.format(p.getStartTime()) + "~" + HM.format(p.getEndTime()) + " 与前面的时段重叠");
                }
                prevEnd = p.getEndTime();
            }
        }
    }

    /**
     * 影响面校验：已预约时段必须仍被新规则覆盖（日期在区间内、非例外日、时段命中），且新容量不小于已约人数；
     * 纯扩容（加容量/加时段/延长时间）不受影响，可正常修改
     */
    private void validateBookedCompatibility(ScheduleRule rule, List<ScheduleSlot> bookedSlots) {
        if (bookedSlots.isEmpty()) {
            return;
        }
        Set<LocalDate> exceptions = collectExceptions(rule);
        for (ScheduleSlot slot : bookedSlots) {
            LocalDate date = slot.getSlotDate();
            LocalTime start = slot.getSlotStartTime();
            ScheduleRulePeriod covering = null;
            if (!date.isBefore(rule.getStartDate()) && !date.isAfter(rule.getEndDate()) && !exceptions.contains(date)) {
                int dayType = isWeekend(date) ? 2 : 1;
                for (ScheduleRulePeriod p : rule.getPeriods()) {
                    if (p.getDayType() == null || p.getDayType() != dayType
                            || p.getStartTime() == null || p.getEndTime() == null) {
                        continue;
                    }
                    boolean hit = !start.isBefore(p.getStartTime()) && start.isBefore(p.getEndTime())
                            && Duration.between(p.getStartTime(), start).toMinutes() % 60 == 0;
                    if (hit) {
                        covering = p;
                        break;
                    }
                }
            }
            String slotLabel = HM.format(start) + "~" + HM.format(slot.getSlotEndTime()) + "（" + date + "）";
            if (covering == null) {
                throw new BusinessException(4002, "时段 " + slotLabel + " 已存在预约，新规则不再覆盖该时段，无法修改");
            }
            if (covering.getCapacity() != null && slot.getBookedCount() != null
                    && covering.getCapacity() < slot.getBookedCount()) {
                throw new BusinessException(4002, "时段 " + slotLabel + " 已预约 " + slot.getBookedCount()
                        + " 人，新上限 " + covering.getCapacity() + " 人小于已约人数，无法修改");
            }
        }
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /** 例外日集合：优先新格式 exceptions，兼容旧 exceptionDates */
    private Set<LocalDate> collectExceptions(ScheduleRule rule) {
        Set<LocalDate> exceptions = new HashSet<>();
        if (rule.getExceptions() != null) {
            rule.getExceptions().stream()
                .filter(e -> e.getExceptionDate() != null)
                .map(ScheduleRuleException::getExceptionDate)
                .forEach(exceptions::add);
        }
        if (exceptions.isEmpty() && rule.getExceptionDates() != null) {
            exceptions.addAll(rule.getExceptionDates());
        }
        return exceptions;
    }

    /**
     * 将每个日类型的第一个时段冗余到主表旧列（兼容列表展示与旧消费方），需在主表写入前调用
     */
    private void applyLegacyPeriodColumns(ScheduleRule rule) {
        if (rule.getPeriods() == null) {
            return;
        }
        for (ScheduleRulePeriod p : rule.getPeriods()) {
            if (p.getDayType() == null) {
                continue;
            }
            if (p.getDayType() == 1 && rule.getWeekdayStartTime() == null) {
                rule.setWeekdayStartTime(p.getStartTime());
                rule.setWeekdayEndTime(p.getEndTime());
                rule.setWeekdayCapacity(p.getCapacity());
            } else if (p.getDayType() == 2 && rule.getWeekendStartTime() == null) {
                rule.setWeekendStartTime(p.getStartTime());
                rule.setWeekendEndTime(p.getEndTime());
                rule.setWeekendCapacity(p.getCapacity());
            }
        }
    }

    /**
     * 保存多时段：写入 schedule_rule_period
     */
    private void savePeriods(ScheduleRule rule) {
        if (rule.getPeriods() == null) {
            return;
        }
        for (ScheduleRulePeriod p : rule.getPeriods()) {
            p.setId(null);
            p.setRuleId(rule.getId());
            periodMapper.insert(p);
        }
    }

    private void saveExceptions(ScheduleRule rule) {
        // 新格式：分批例外日（带备注）；兼容旧格式 exceptionDates（无备注）
        List<ScheduleRuleException> list = rule.getExceptions();
        if ((list == null || list.isEmpty()) && rule.getExceptionDates() != null) {
            list = rule.getExceptionDates().stream().map(d -> {
                ScheduleRuleException e = new ScheduleRuleException();
                e.setExceptionDate(d);
                return e;
            }).toList();
        }
        if (list == null) {
            return;
        }
        for (ScheduleRuleException e : list) {
            LocalDate date = e.getExceptionDate();
            if (date == null) {
                continue;
            }
            if (date.isBefore(rule.getStartDate()) || date.isAfter(rule.getEndDate())) {
                throw new BusinessException(400, "例外日 " + date + " 不在排班区间内");
            }
            e.setId(null);
            e.setRuleId(rule.getId());
            exceptionMapper.insert(e);
        }
    }

    /**
     * 按规则物化生成每日时段：每个日类型的多条时段各自按整小时切分；例外日跳过；
     * preserve 中的已预约时段（日期+开始时间）保留原记录，不重复生成
     */
    private void materializeSlots(ScheduleRule rule, List<ScheduleSlot> preserve) {
        Set<LocalDate> exceptions = collectExceptions(rule);
        Set<String> preserved = new HashSet<>();
        for (ScheduleSlot s : preserve) {
            preserved.add(s.getSlotDate() + "#" + HM.format(s.getSlotStartTime()));
        }
        List<ScheduleRulePeriod> periods = rule.getPeriods() == null ? List.of() : rule.getPeriods();
        for (LocalDate date = rule.getStartDate(); !date.isAfter(rule.getEndDate()); date = date.plusDays(1)) {
            if (exceptions.contains(date)) {
                continue; // 例外日不生成时段
            }
            int dayType = isWeekend(date) ? 2 : 1;
            for (ScheduleRulePeriod p : periods) {
                if (p.getDayType() == null || p.getDayType() != dayType) {
                    continue;
                }
                for (LocalTime t = p.getStartTime(); t.isBefore(p.getEndTime()); t = t.plusHours(1)) {
                    if (preserved.contains(date + "#" + HM.format(t))) {
                        continue; // 已预约时段保留，避免唯一键冲突
                    }
                    ScheduleSlot slot = new ScheduleSlot();
                    slot.setStoreId(rule.getStoreId());
                    slot.setSlotDate(date);
                    slot.setSlotStartTime(t);
                    slot.setSlotEndTime(t.plusHours(1));
                    slot.setMaxCapacity(p.getCapacity());
                    slot.setBookedCount(0);
                    slot.setStatus(1);
                    slotMapper.insert(slot);
                }
            }
        }
    }
}
