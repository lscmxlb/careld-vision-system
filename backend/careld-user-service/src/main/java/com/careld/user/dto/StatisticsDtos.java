package com.careld.user.dto;

import lombok.Data;

import java.util.List;

/**
 * 统计相关响应 DTO 集合
 */
public final class StatisticsDtos {

    private StatisticsDtos() {
    }

    @Data
    public static class DashboardStats {
        private long todayReserves;
        private long pendingChildren;
        private long activeDevices;
        private long todayTests;
    }

    @Data
    public static class StoreTraffic {
        private TrafficSummary summary;
        private List<TrafficDetail> details;

        @Data
        public static class TrafficSummary {
            private long totalVisits;
            private long avgDaily;
            private long maxDaily;
            private String growthRate;
        }

        @Data
        public static class TrafficDetail {
            private String date;
            private long visitCount;
            private long newChildren;
            private long returnChildren;
        }
    }

    @Data
    public static class VisionImprovement {
        private Long storeId;
        private String storeName;
        private long totalTests;
        private long improvedCount;
        private String improvementRate;
        private String avgImprovement;
    }

    @Data
    public static class NationalSummary {
        private long storeCount;
        private long totalChildren;
        private long monthlyVisits;
        private String avgImprovement;
        private List<TopStore> topStores;

        @Data
        public static class TopStore {
            private Long storeId;
            private String storeName;
            private long visitCount;
            private String improvementRate;
        }
    }

    @Data
    public static class WeeklyTrend {
        private List<String> dates;
        private List<Long> reserveCounts;
        private List<Long> testCounts;
    }

    @Data
    public static class VisionStatistics {
        private List<String> labels;
        private List<Long> beforeValues;
        private List<Long> afterValues;
    }

    @Data
    public static class ExportResponse {
        private String downloadUrl;
    }
}
