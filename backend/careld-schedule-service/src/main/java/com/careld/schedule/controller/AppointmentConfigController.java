package com.careld.schedule.controller;

import com.careld.common.exception.BusinessException;
import com.careld.common.result.Result;
import com.careld.common.security.DataScopeHelper;
import com.careld.common.security.UserContext;
import com.careld.schedule.entity.AppointmentConfig;
import com.careld.schedule.service.ScheduleRuleService;
import com.careld.common.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 预约规则配置（家长取消时间窗 / 医生取消时间窗 / 爽约缓冲 / 自动完成时长）
 */
@Tag(name = "预约规则设置")
@RestController
@RequestMapping("/api/v1/appointment-config")
@RequiredArgsConstructor
public class AppointmentConfigController {

    private final ScheduleRuleService scheduleRuleService;

    @Operation(summary = "读取预约规则配置（无配置返回默认值）")
    @GetMapping
    public Result<AppointmentConfig> get(@RequestParam(value = "storeId", required = false) Long storeId) {
        return Result.success(scheduleRuleService.getConfig(DataScopeHelper.resolveStoreIdWithParentChoice(storeId)));
    }

    @OperationLog(module = "appointment-config", action = "update", description = "保存预约规则")
    @Operation(summary = "保存预约规则配置")
    @PutMapping
    public Result<Void> save(@RequestBody AppointmentConfig config) {
        if (config.getStoreId() == null) {
            config.setStoreId(UserContext.getCurrentStoreId());
        }
        if (config.getStoreId() == null) {
            throw new BusinessException(400, "医院不能为空");
        }
        scheduleRuleService.saveConfig(config);
        return Result.success();
    }
}
