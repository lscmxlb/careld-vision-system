package com.careld.schedule.controller;

import com.careld.common.exception.BusinessException;
import com.careld.common.result.Result;
import com.careld.common.security.DataScopeHelper;
import com.careld.common.security.UserContext;
import com.careld.schedule.entity.ScheduleRule;
import com.careld.schedule.entity.ScheduleSlot;
import com.careld.schedule.service.ScheduleRuleService;
import com.careld.common.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 排班规则 / 可约时段
 */
@Tag(name = "排班规则管理")
@RestController
@RequestMapping("/api/v1/schedule-rules")
@RequiredArgsConstructor
public class ScheduleRuleController {

    private final ScheduleRuleService scheduleRuleService;

    @OperationLog(module = "schedule-rules", action = "create", description = "新增排班规则")
    @Operation(summary = "创建排班规则")
    @PostMapping
    public Result<Long> create(@RequestBody ScheduleRule rule) {
        if (rule.getStoreId() == null) {
            rule.setStoreId(UserContext.getCurrentStoreId());
        }
        if (rule.getStoreId() == null) {
            throw new BusinessException(400, "医院不能为空");
        }
        return Result.success(scheduleRuleService.createRule(rule));
    }

    @OperationLog(module = "schedule-rules", action = "update", description = "修改排班规则")
    @Operation(summary = "修改排班规则")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScheduleRule rule) {
        scheduleRuleService.updateRule(id, rule);
        return Result.success();
    }

    @OperationLog(module = "schedule-rules", action = "delete", description = "删除排班规则")
    @Operation(summary = "删除排班规则")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleRuleService.deleteRule(id);
        return Result.success();
    }

    @Operation(summary = "排班规则列表（含例外日）")
    @GetMapping
    public Result<List<ScheduleRule>> list(@RequestParam(value = "storeId", required = false) Long storeId) {
        return Result.success(scheduleRuleService.listRules(DataScopeHelper.resolveStoreId(storeId)));
    }

    @Operation(summary = "查询某日期的可约时段")
    @GetMapping("/slots")
    public Result<List<ScheduleSlot>> slots(@RequestParam(value = "storeId", required = false) Long storeId,
                                            @RequestParam("date") LocalDate date) {
        return Result.success(scheduleRuleService.getSlots(DataScopeHelper.resolveStoreIdWithParentChoice(storeId), date));
    }

    @Operation(summary = "查询日期范围内的可约日期（去重）")
    @GetMapping("/available-dates")
    public Result<List<LocalDate>> availableDates(@RequestParam(value = "storeId", required = false) Long storeId,
                                                  @RequestParam("startDate") LocalDate startDate,
                                                  @RequestParam("endDate") LocalDate endDate) {
        return Result.success(scheduleRuleService.getAvailableDates(DataScopeHelper.resolveStoreIdWithParentChoice(storeId), startDate, endDate));
    }

    @Operation(summary = "按月聚合每日名额（日历：A=已约 B=当天可接受预约总数，B 不随已约变化）")
    @GetMapping("/slot-daily-summary")
    public Result<List<Map<String, Object>>> slotDailySummary(@RequestParam(value = "storeId", required = false) Long storeId,
                                                              @RequestParam("startDate") LocalDate startDate,
                                                              @RequestParam("endDate") LocalDate endDate) {
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        if (effectiveStoreId == null) {
            throw new BusinessException(400, "医院不能为空");
        }
        return Result.success(scheduleRuleService.getSlotDailySummary(effectiveStoreId, startDate, endDate));
    }
}
