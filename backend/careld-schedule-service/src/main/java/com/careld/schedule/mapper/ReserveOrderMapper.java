package com.careld.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.schedule.entity.ReserveOrder;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReserveOrderMapper extends BaseMapper<ReserveOrder> {

    /** 养护提醒候选预约（仅取提醒所需字段） */
    @Data
    class ReminderCandidate {
        private Long id;
        private Long storeId;
        private Long childId;
        private LocalDate reserveDate;
        private LocalTime reserveTimeStart;
        private LocalTime reserveTimeEnd;
    }

    /**
     * 预约统计：起止日期内每日预约总数、已养护数、取消数、待养护数
     */
    @Select("SELECT reserve_date AS statDate, COUNT(*) AS total, "
            + "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS completed, "
            + "SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) AS cancelled, "
            + "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS pending "
            + "FROM reserve_order WHERE deleted_at IS NULL "
            + "AND reserve_date BETWEEN #{startDate} AND #{endDate} "
            + "AND (#{storeId} IS NULL OR store_id = #{storeId}) "
            + "GROUP BY reserve_date ORDER BY reserve_date")
    List<Map<String, Object>> statisticsByDateRange(@Param("storeId") Long storeId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * 养护提醒候选：待到店、开始时间落在 [from, until]（即「开始时间 - 提前量」刚过去不久），
     * 且预约创建时间不晚于「开始时间 - 提前量」。
     * <p>创建晚于提醒时刻的预约不补发提醒（否则刚下单就会连收两条短信）；
     * 同一预约同一日期时段已生成过提醒任务时不再重复生成（{@code reserveDate}/{@code timeStart} 出现在任务载荷里）。</p>
     */
    @Select("SELECT o.id AS id, o.store_id AS storeId, o.child_id AS childId, "
            + "o.reserve_date AS reserveDate, o.reserve_time_start AS reserveTimeStart, "
            + "o.reserve_time_end AS reserveTimeEnd "
            + "FROM reserve_order o "
            + "WHERE o.deleted_at IS NULL AND o.status = 1 "
            + "AND o.reserve_date BETWEEN DATE(#{from}) AND DATE(#{until}) "
            + "AND o.reserve_time_start IS NOT NULL "
            + "AND TIMESTAMP(o.reserve_date, o.reserve_time_start) BETWEEN #{from} AND #{until} "
            + "AND TIMESTAMP(o.reserve_date, o.reserve_time_start) >= DATE_ADD(o.created_at, INTERVAL #{minutes} MINUTE) "
            + "AND NOT EXISTS (SELECT 1 FROM notify_task t "
            + "  WHERE t.event_type = 'care_reminder' AND t.ref_id = o.id AND t.deleted_at IS NULL "
            + "  AND t.payload LIKE CONCAT('%', DATE_FORMAT(o.reserve_date, '%Y-%m-%d'), '%') "
            + "  AND t.payload LIKE CONCAT('%', DATE_FORMAT(o.reserve_time_start, '%H:%i'), '%')) "
            + "ORDER BY o.id LIMIT 200")
    List<ReminderCandidate> selectReminderCandidates(@Param("from") LocalDateTime from,
                                                    @Param("until") LocalDateTime until,
                                                    @Param("minutes") int minutes);

    /** 读取系统参数（养护提醒提前分钟数等） */
    @Select("SELECT config_value FROM sys_config WHERE config_key = #{key} LIMIT 1")
    String selectConfigValue(@Param("key") String key);
}
