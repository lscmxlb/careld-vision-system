package com.careld.schedule.controller;

import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.common.security.UserContext;
import com.careld.common.security.DataScopeHelper;
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
        // 数据权限：门店用户注入 storeId
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        return Result.success(scheduleService.getCalendar(effectiveStoreId, startDate, endDate));
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
    public Result<PageResult<ReserveOrder>> listReserves(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "childId", required = false) Long childId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "statuses", required = false) List<Integer> statuses,
            @RequestParam(value = "noShowFlag", required = false) Boolean noShowFlag,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 家长数据以本人孩子为准：不带具体孩子时不返回任何预约，避免跨院串看
        if (DataScopeHelper.isParent() && childId == null) {
            return Result.success(PageResult.of(List.of(), page, size, 0));
        }
        // 数据权限：家长按所选医院（可为异地），其他角色注入 storeId
        Long effectiveStoreId = DataScopeHelper.resolveStoreIdWithParentChoice(storeId);
        var p = scheduleService.listReserves(effectiveStoreId, childId, status, statuses, noShowFlag, date, startDate, keyword, page, size);
        return Result.success(PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal()));
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

    @Operation(summary = "创建预约（新链路：slotId + childId，校验审核/次数/满额/每日一约）")
    @PostMapping("/reserves/v2")
    public Result<Long> createReserveV2(@RequestBody ReserveOrder order) {
        return Result.success(scheduleService.createReserveV2(order));
    }

    @Operation(summary = "预约统计（每日预约数/完成数/取消数）")
    @GetMapping("/reserves/statistics")
    public Result<List<Map<String, Object>>> statistics(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        return Result.success(scheduleService.statisticsReserves(effectiveStoreId, startDate, endDate));
    }

    @Operation(summary = "开始养护（录入养护前视力）")
    @PostMapping("/reserves/{id}/start")
    public Result<Void> startCare(@PathVariable Long id, @RequestBody Map<String, String> params) {
        scheduleService.startCare(id, params);
        return Result.success();
    }

    @Operation(summary = "完成养护（录入养护后视力）")
    @PostMapping("/reserves/{id}/complete")
    public Result<Void> completeCare(@PathVariable Long id, @RequestBody Map<String, String> params) {
        scheduleService.completeCare(id, params);
        return Result.success();
    }

    @Operation(summary = "查询预约的养护记录（养护记录登记弹窗回填）")
    @GetMapping("/reserves/{id}/care-record")
    public Result<com.careld.schedule.entity.CareRecord> getCareRecord(@PathVariable Long id) {
        return Result.success(scheduleService.getCareRecord(id));
    }

    @Operation(summary = "标记爽约（不退还预约次数）")
    @PostMapping("/reserves/{id}/no-show")
    public Result<Void> noShow(@PathVariable Long id) {
        scheduleService.markNoShow(id);
        return Result.success();
    }

    @Operation(summary = "预约调整（已预约记录更换到新时段）")
    @PostMapping("/reserves/{id}/adjust")
    public Result<Void> adjust(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Object slotId = params.get("slotId");
        Long newSlotId = slotId == null ? null : Long.valueOf(String.valueOf(slotId));
        scheduleService.adjustReserve(id, newSlotId);
        return Result.success();
    }
}

