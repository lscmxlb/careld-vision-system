package com.careld.user.service.impl;

import com.careld.user.dto.StatisticsDtos;
import com.careld.user.mapper.StatisticsMapper;
import com.careld.user.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务实现（基于共享库跨表 SQL 聚合）
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    @Override
    public StatisticsDtos.DashboardStats dashboard(Long storeId) {
        return statisticsMapper.dashboard(storeId);
    }

    @Override
    public StatisticsDtos.StoreTraffic storeTraffic(Long storeId, LocalDate startDate, LocalDate endDate, String groupBy) {
        String bucket = groupBy == null ? "day" : groupBy;
        List<Map<String, Object>> rows = statisticsMapper.trafficDetails(storeId, startDate, endDate, bucket);

        StatisticsDtos.StoreTraffic traffic = new StatisticsDtos.StoreTraffic();
        StatisticsDtos.StoreTraffic.TrafficSummary summary = new StatisticsDtos.StoreTraffic.TrafficSummary();

        long totalVisits = statisticsMapper.trafficTotal(storeId, startDate, endDate);
        long dayCount = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate) + 1);
        long maxDaily = 0;
        List<StatisticsDtos.StoreTraffic.TrafficDetail> details = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            StatisticsDtos.StoreTraffic.TrafficDetail d = new StatisticsDtos.StoreTraffic.TrafficDetail();
            d.setDate(String.valueOf(row.get("date")));
            long visitCount = toLong(row.get("visitCount"));
            long newChildren = toLong(row.get("newChildren"));
            d.setVisitCount(visitCount);
            d.setNewChildren(newChildren);
            d.setReturnChildren(Math.max(0, visitCount - newChildren));
            maxDaily = Math.max(maxDaily, visitCount);
            details.add(d);
        }
        summary.setTotalVisits(totalVisits);
        summary.setAvgDaily(totalVisits / dayCount);
        summary.setMaxDaily(maxDaily);

        // 增长率：对比前一等长周期
        long priorDays = dayCount;
        LocalDate priorStart = startDate.minusDays(priorDays);
        LocalDate priorEnd = startDate.minusDays(1);
        long priorVisits = statisticsMapper.trafficCount(storeId, priorStart, priorEnd);
        summary.setGrowthRate(growthRate(totalVisits, priorVisits));

        traffic.setSummary(summary);
        traffic.setDetails(details);
        return traffic;
    }

    @Override
    public StatisticsDtos.VisionImprovement visionImprovement(Long storeId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = statisticsMapper.visionImprovementByChild(storeId, startDate, endDate.plusDays(1));
        StatisticsDtos.VisionImprovement result = new StatisticsDtos.VisionImprovement();
        result.setStoreId(storeId);
        if (storeId != null) {
            result.setStoreName(statisticsMapper.storeName(storeId));
        }

        long totalTests = 0;
        long improvedCount = 0;
        long comparable = 0;
        double improvementSum = 0.0;
        for (Map<String, Object> row : rows) {
            totalTests += toLong(row.get("cnt"));
            BigDecimal before = toBigDecimal(row.get("beforeVal"));
            BigDecimal after = toBigDecimal(row.get("afterVal"));
            if (before != null && after != null) {
                comparable++;
                double diff = after.doubleValue() - before.doubleValue();
                if (diff > 0) {
                    improvedCount++;
                }
                improvementSum += diff;
            }
        }
        result.setTotalTests(totalTests);
        result.setImprovedCount(improvedCount);
        result.setImprovementRate(comparable == 0 ? "0.0%" : formatRate((double) improvedCount / comparable * 100));
        result.setAvgImprovement(comparable == 0 ? "0.00" : String.format("%.2f", improvementSum / comparable));
        return result;
    }

    @Override
    public StatisticsDtos.NationalSummary nationalSummary(LocalDate startDate, LocalDate endDate) {
        StatisticsDtos.NationalSummary summary = new StatisticsDtos.NationalSummary();
        summary.setStoreCount(statisticsMapper.storeCount());
        summary.setTotalChildren(statisticsMapper.totalChildren());
        summary.setMonthlyVisits(statisticsMapper.trafficCount(null, startDate, endDate));

        // 全国平均改善
        StatisticsDtos.VisionImprovement vi = visionImprovement(null, startDate, endDate);
        summary.setAvgImprovement(vi.getAvgImprovement());

        List<Map<String, Object>> topRows = statisticsMapper.topStores(startDate, endDate, 5);
        // 各门店改善率
        Map<Long, String> rateMap = new HashMap<>();
        List<Map<String, Object>> storeImprov = statisticsMapper.visionImprovementByChild(null, startDate, endDate.plusDays(1));
        Map<Long, long[]> agg = new HashMap<>(); // storeId -> [improved, comparable]
        // child 维度不含 storeId，需另查 child->store；此处用全国聚合近似
        long totalImproved = 0;
        long totalComparable = 0;
        for (Map<String, Object> row : storeImprov) {
            BigDecimal before = toBigDecimal(row.get("beforeVal"));
            BigDecimal after = toBigDecimal(row.get("afterVal"));
            if (before != null && after != null) {
                totalComparable++;
                if (after.doubleValue() - before.doubleValue() > 0) {
                    totalImproved++;
                }
            }
        }
        String globalRate = totalComparable == 0 ? "0.0%" : formatRate((double) totalImproved / totalComparable * 100);

        List<StatisticsDtos.NationalSummary.TopStore> topStores = new ArrayList<>();
        for (Map<String, Object> row : topRows) {
            StatisticsDtos.NationalSummary.TopStore t = new StatisticsDtos.NationalSummary.TopStore();
            t.setStoreId(toLongObj(row.get("storeId")));
            t.setStoreName(String.valueOf(row.get("storeName")));
            t.setVisitCount(toLong(row.get("visitCount")));
            t.setImprovementRate(globalRate);
            topStores.add(t);
        }
        summary.setTopStores(topStores);
        return summary;
    }

    @Override
    public StatisticsDtos.WeeklyTrend weeklyTrend(Long storeId, String period) {
        int days = "month".equalsIgnoreCase(period) ? 30 : 7;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);

        List<Map<String, Object>> reserveRows = statisticsMapper.reserveTrend(storeId, start, end);
        List<Map<String, Object>> testRows = statisticsMapper.testTrend(storeId, start, end.plusDays(1));
        Map<String, Long> reserveMap = toCountMap(reserveRows);
        Map<String, Long> testMap = toCountMap(testRows);

        List<String> dates = new ArrayList<>();
        List<Long> reserveCounts = new ArrayList<>();
        List<Long> testCounts = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dates.add(d.toString());
            reserveCounts.add(reserveMap.getOrDefault(d.toString(), 0L));
            testCounts.add(testMap.getOrDefault(d.toString(), 0L));
        }
        StatisticsDtos.WeeklyTrend trend = new StatisticsDtos.WeeklyTrend();
        trend.setDates(dates);
        trend.setReserveCounts(reserveCounts);
        trend.setTestCounts(testCounts);
        return trend;
    }

    @Override
    public StatisticsDtos.VisionStatistics visionStats(Long storeId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = statisticsMapper.visionWeekly(storeId, startDate, endDate.plusDays(1));
        List<String> labels = new ArrayList<>();
        List<Long> beforeValues = new ArrayList<>();
        List<Long> afterValues = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            labels.add(String.valueOf(row.get("week")));
            beforeValues.add(scaleLong(toBigDecimal(row.get("beforeAvg"))));
            afterValues.add(scaleLong(toBigDecimal(row.get("afterAvg"))));
        }
        StatisticsDtos.VisionStatistics stats = new StatisticsDtos.VisionStatistics();
        stats.setLabels(labels);
        stats.setBeforeValues(beforeValues);
        stats.setAfterValues(afterValues);
        return stats;
    }

    @Override
    public StatisticsDtos.ExportResponse export(String exportType, Long storeId, LocalDate startDate, LocalDate endDate, String format) {
        // 导出：返回虚拟下载链接（生产可对接 OSS / 本地静态目录）
        StatisticsDtos.ExportResponse resp = new StatisticsDtos.ExportResponse();
        String params = "type=" + (exportType == null ? "dashboard" : exportType)
                + "&storeId=" + (storeId == null ? "" : storeId)
                + "&start=" + startDate + "&end=" + endDate + "&fmt=" + (format == null ? "csv" : format);
        resp.setDownloadUrl("/api/v1/statistics/download?" + params);
        return resp;
    }

    // ---------- helpers ----------

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    private Long toLongObj(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return null; }
    }

    private long scaleLong(BigDecimal v) {
        if (v == null) return 0L;
        // 视力值（如 4.85）放大 100 倍为整数，便于图表展示
        return v.multiply(new BigDecimal("100")).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(String.valueOf(row.get("date")), toLong(row.get("cnt")));
        }
        return map;
    }

    private String growthRate(long current, long prior) {
        if (prior == 0) {
            return current == 0 ? "0.0%" : "+100.0%";
        }
        double rate = (double) (current - prior) / prior * 100;
        return (rate >= 0 ? "+" : "") + String.format("%.1f", rate) + "%";
    }

    private String formatRate(double percent) {
        return String.format("%.1f", percent) + "%";
    }
}
