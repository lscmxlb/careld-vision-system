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
        /** 本月预约（按预约日期，含已取消） */
        private long monthReserveCount;
        /** 总预约数量（含已取消） */
        private long totalReserveCount;
        /** 本月新增档案（按建档时间） */
        private long monthChildCount;
        /** 档案总数（未删除且非已隐藏） */
        private long totalChildCount;
        /** 本月养护（按养护日期） */
        private long monthCareCount;
        /** 总养护数量 */
        private long totalCareCount;
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

    /**
     * 医生端工作台统计：儿童档案 / 本月预约 / 养护次数
     */
    @Data
    public static class WorkbenchStats {
        /** 儿童档案数量（未删除且非已隐藏） */
        private long childCount;
        /** 本月预约数量（预约日期在本月且状态为已预约） */
        private long reservedCount;
        /** 养护次数（养护记录已完成） */
        private long completedCareCount;
    }

    /**
     * 医院数据统计：医院基本信息 + 动态统计
     */
    @Data
    public static class StoreOverview {
        // ---- 医院基本信息 ----
        private Long storeId;
        private String storeCode;
        private String storeName;
        private Integer institutionType;
        private Integer status;
        private String provinceName;
        private String cityName;
        private String districtName;
        private String address;
        private String contactName;
        private String contactPhone;
        private String businessHours;
        private String joinDate;
        private Integer bedCount;
        private String agentName;
        private String centerName;

        // ---- 动态统计 ----
        /** 儿童档案数量（未删除且非已隐藏） */
        private Long childCount;
        /** 养护服务次数（含养护中） */
        private Long careCount;
        /** 本月预约数据（按预约日期，排除已取消） */
        private Long monthlyReserveCount;
        /** 最后活跃日期（医生/医生助理最后登录，无记录为空） */
        private String lastActiveDate;
    }
}
