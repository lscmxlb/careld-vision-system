package com.careld.schedule.controller;

import com.careld.common.result.Result;
import com.careld.common.security.UserContext;
import com.careld.schedule.dto.BatchScheduleRequest;
import com.careld.schedule.entity.ReserveOrder;
import com.careld.schedule.entity.Schedule;
import com.careld.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 预约排班管理控制器
 */
@Tag(name = "预约排班管理")
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "排班日历")
    @GetMapping("/calendar")
    public Result<List<Schedule>> calendar(@RequestParam("storeId") Long storeId,
                                           @RequestParam("startDate") LocalDate startDate,
                                           @RequestParam("endDate") LocalDate endDate) {
        return Result.success(scheduleService.getCalendar(storeId, startDate, endDate));
    }

    @Operation(summary = "创建排班")
    @PostMapping
    public Result<Long> create(@RequestBody Schedule schedule) {
        return Result.success(scheduleService.createSchedule(schedule));
    }

    @Operation(summary = "批量创建排班")
    @PostMapping("/batch")
    public Result<Void> batchCreate(@RequestBody BatchScheduleRequest request) {
        if (request.getStoreId() == null) {
            request.setStoreId(UserContext.getCurrentStoreId());
        }
        scheduleService.batchCreate(request);
        return Result.success();
    }

    @Operation(summary = "更新排班")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Schedule schedule) {
        scheduleService.updateSchedule(id, schedule);
        return Result.success();
    }

    @Operation(summary = "删除排班")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return Result.success();
    }

    @Operation(summary = "预约列表")
    @GetMapping("/reserves")
    public Result<List<ReserveOrder>> listReserves(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "childId", required = false) Long childId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "date", required = false) LocalDate date) {
        return Result.success(scheduleService.listReserves(storeId, childId, status, date));
    }

    @Operation(summary = "预约详情")
    @GetMapping("/reserves/{id}")
    public Result<ReserveOrder> getReserve(@PathVariable Long id) {
        return Result.success(scheduleService.getReserveById(id));
    }

    @Operation(summary = "创建预约")
    @PostMapping("/reserves")
    public Result<Long> createReserve(@RequestBody ReserveOrder order) {
        return Result.success(scheduleService.createReserve(order));
    }

    @Operation(summary = "取消预约")
    @PostMapping("/reserves/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody Map<String, String> params) {
        scheduleService.cancelReserve(id, params.get("cancelReason"));
        return Result.success();
    }
}

