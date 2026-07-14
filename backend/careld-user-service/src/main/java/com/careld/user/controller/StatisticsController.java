package com.careld.user.controller;

import com.careld.common.result.Result;
import com.careld.user.dto.StatisticsDtos;
import com.careld.user.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 数据统计控制器
 */
@Tag(name = "数据统计")
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "看板统计")
    @GetMapping("/dashboard")
    public Result<StatisticsDtos.DashboardStats> dashboard(@RequestParam(value = "storeId", required = false) Long storeId) {
        return Result.success(statisticsService.dashboard(storeId));
    }

    @Operation(summary = "门店客流统计")
    @GetMapping("/store-traffic")
    public Result<StatisticsDtos.StoreTraffic> storeTraffic(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate,
            @RequestParam(value = "groupBy", defaultValue = "day") String groupBy) {
        return Result.success(statisticsService.storeTraffic(storeId, startDate, endDate, groupBy));
    }

    @Operation(summary = "视力改善统计")
    @GetMapping("/vision-improvement")
    public Result<StatisticsDtos.VisionImprovement> visionImprovement(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return Result.success(statisticsService.visionImprovement(storeId, startDate, endDate));
    }

    @Operation(summary = "全国门店数据汇总")
    @GetMapping("/national-summary")
    public Result<StatisticsDtos.NationalSummary> nationalSummary(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return Result.success(statisticsService.nationalSummary(startDate, endDate));
    }

    @Operation(summary = "预约/检测趋势")
    @GetMapping("/weekly-trend")
    public Result<StatisticsDtos.WeeklyTrend> weeklyTrend(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "period", defaultValue = "week") String period) {
        return Result.success(statisticsService.weeklyTrend(storeId, period));
    }

    @Operation(summary = "视力检测统计")
    @GetMapping("/vision-stats")
    public Result<StatisticsDtos.VisionStatistics> visionStats(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return Result.success(statisticsService.visionStats(storeId, startDate, endDate));
    }

    @Operation(summary = "数据导出")
    @PostMapping("/export")
    public Result<StatisticsDtos.ExportResponse> export(@RequestBody Map<String, Object> body) {
        String exportType = body.get("exportType") == null ? null : String.valueOf(body.get("exportType"));
        Long storeId = body.get("storeId") == null ? null : Long.valueOf(body.get("storeId").toString());
        LocalDate startDate = LocalDate.parse(String.valueOf(body.get("startDate")));
        LocalDate endDate = LocalDate.parse(String.valueOf(body.get("endDate")));
        String format = body.get("format") == null ? "csv" : String.valueOf(body.get("format"));
        return Result.success(statisticsService.export(exportType, storeId, startDate, endDate, format));
    }
}
