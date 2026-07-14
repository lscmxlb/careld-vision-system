package com.careld.user.service;

import com.careld.user.dto.StatisticsDtos;

import java.time.LocalDate;

/**
 * 数据统计服务
 */
public interface StatisticsService {

    StatisticsDtos.DashboardStats dashboard(Long storeId);

    StatisticsDtos.StoreTraffic storeTraffic(Long storeId, LocalDate startDate, LocalDate endDate, String groupBy);

    StatisticsDtos.VisionImprovement visionImprovement(Long storeId, LocalDate startDate, LocalDate endDate);

    StatisticsDtos.NationalSummary nationalSummary(LocalDate startDate, LocalDate endDate);

    StatisticsDtos.WeeklyTrend weeklyTrend(Long storeId, String period);

    StatisticsDtos.VisionStatistics visionStats(Long storeId, LocalDate startDate, LocalDate endDate);

    StatisticsDtos.ExportResponse export(String exportType, Long storeId, LocalDate startDate, LocalDate endDate, String format);
}
