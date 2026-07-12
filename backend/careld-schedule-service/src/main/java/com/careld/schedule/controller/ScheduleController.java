package com.careld.schedule.controller;
import com.careld.common.result.Result;
import com.careld.schedule.entity.ReserveOrder;
import com.careld.schedule.entity.Schedule;
import com.careld.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
@Tag(name = "预约排班管理")
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;
    @GetMapping("/calendar")
    public Result<List<Schedule>> calendar(@RequestParam("storeId") Long storeId,
                                           @RequestParam("startDate") LocalDate startDate,
                                           @RequestParam("endDate") LocalDate endDate) {
        return Result.success(scheduleService.getCalendar(storeId, startDate, endDate));
    }
    @PostMapping
    public Result<Long> create(@RequestBody Schedule schedule) {
        return Result.success(scheduleService.createSchedule(schedule));
    }
    @PostMapping("/batch")
    public Result<Void> batchCreate(@RequestBody Map<String, Object> params) {
        return Result.success();
    }
    @GetMapping("/reserves")
    public Result<List<ReserveOrder>> listReserves(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "childId", required = false) Long childId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "date", required = false) LocalDate date) {
        return Result.success(scheduleService.listReserves(storeId, childId, status, date));
    }
    @PostMapping("/reserves")
    public Result<Long> createReserve(@RequestBody ReserveOrder order) {
        return Result.success(scheduleService.createReserve(order));
    }
    @PostMapping("/reserves/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody Map<String, String> params) {
        scheduleService.cancelReserve(id, params.get("cancelReason")); return Result.success();
    }
}
