package com.careld.user.mapper;

import com.careld.user.dto.StatisticsDtos;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计 Mapper（直连共享库 careld_vision 跨表聚合）
 */
@Mapper
public interface StatisticsMapper {

    /**
     * 看板统计：今日预约 / 待审核档案 / 在线设备 / 今日检测
     */
    @Select("SELECT "
            + "(SELECT COUNT(*) FROM reserve_order WHERE reserve_date = CURDATE() "
            + "  AND (#{storeId} IS NULL OR store_id = #{storeId})) AS todayReserves, "
            + "(SELECT COUNT(*) FROM child_profile WHERE audit_status = 0 AND deleted_at IS NULL "
            + "  AND (#{storeId} IS NULL OR store_id = #{storeId})) AS pendingChildren, "
            + "(SELECT COUNT(*) FROM store_tv_device WHERE status = 1 "
            + "  AND (#{storeId} IS NULL OR store_id = #{storeId})) AS activeDevices, "
            + "(SELECT COUNT(*) FROM vision_test_record WHERE DATE(created_at) = CURDATE() "
            + "  AND (#{storeId} IS NULL OR store_id = #{storeId})) AS todayTests")
    StatisticsDtos.DashboardStats dashboard(@Param("storeId") Long storeId);

    /**
     * 门店客流明细（按天/周/月聚合）
     */
    @Select("<script>"
            + "SELECT "
            + "<choose>"
            + "<when test=\"groupBy == 'week'\">DATE(DATE_SUB(reserve_date, INTERVAL WEEKDAY(reserve_date) DAY))</when>"
            + "<when test=\"groupBy == 'month'\">DATE_FORMAT(reserve_date, '%Y-%m-01')</when>"
            + "<otherwise>DATE(reserve_date)</otherwise>"
            + "</choose> AS date, "
            + "COUNT(*) AS visitCount, "
            + "SUM(CASE WHEN reserve_date = (SELECT MIN(reserve_date) FROM reserve_order r2 WHERE r2.child_id = reserve_order.child_id) THEN 1 ELSE 0 END) AS newChildren "
            + "FROM reserve_order "
            + "WHERE reserve_date BETWEEN #{startDate} AND #{endDate} "
            + "AND (#{storeId} IS NULL OR store_id = #{storeId}) "
            + "GROUP BY date ORDER BY date"
            + "</script>")
    List<Map<String, Object>> trafficDetails(@Param("storeId") Long storeId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              @Param("groupBy") String groupBy);

    @Select("SELECT COUNT(*) FROM reserve_order "
            + "WHERE reserve_date BETWEEN #{startDate} AND #{endDate} "
            + "AND (#{storeId} IS NULL OR store_id = #{storeId})")
    long trafficTotal(@Param("storeId") Long storeId,
                      @Param("startDate") LocalDate startDate,
                      @Param("endDate") LocalDate endDate);

    @Select("SELECT COUNT(*) FROM reserve_order "
            + "WHERE reserve_date BETWEEN #{startDate} AND #{endDate} "
            + "AND (#{storeId} IS NULL OR store_id = #{storeId})")
    long trafficCount(@Param("storeId") Long storeId,
                      @Param("startDate") LocalDate startDate,
                      @Param("endDate") LocalDate endDate);

    /**
     * 视力改善：按 child 聚合养护前(MIN)/养护后(MAX)视力小数
     */
    @Select("SELECT child_id, "
            + "MIN(CASE WHEN test_type = 1 THEN vision_decimal END) AS beforeVal, "
            + "MAX(CASE WHEN test_type = 2 THEN vision_decimal END) AS afterVal, "
            + "COUNT(*) AS cnt "
            + "FROM vision_test_record "
            + "WHERE created_at >= #{startDate} AND created_at < #{endDatePlusOne} "
            + "AND (#{storeId} IS NULL OR store_id = #{storeId}) "
            + "GROUP BY child_id")
    List<Map<String, Object>> visionImprovementByChild(@Param("storeId") Long storeId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDatePlusOne") LocalDate endDatePlusOne);

    /**
     * 门店名称
     */
    @Select("SELECT store_name FROM store_info WHERE id = #{storeId} AND deleted_at IS NULL")
    String storeName(@Param("storeId") Long storeId);

    /**
     * 全国门店客流 TopN（含门店名）
     */
    @Select("SELECT s.id AS storeId, s.store_name AS storeName, COUNT(r.id) AS visitCount "
            + "FROM store_info s "
            + "LEFT JOIN reserve_order r ON r.store_id = s.id AND r.reserve_date BETWEEN #{startDate} AND #{endDate} "
            + "WHERE s.deleted_at IS NULL "
            + "GROUP BY s.id, s.store_name "
            + "ORDER BY visitCount DESC LIMIT #{top}")
    List<Map<String, Object>> topStores(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("top") int top);

    @Select("SELECT COUNT(*) FROM store_info WHERE deleted_at IS NULL")
    long storeCount();

    @Select("SELECT COUNT(*) FROM child_profile WHERE deleted_at IS NULL")
    long totalChildren();

    /**
     * 周期内每日预约数
     */
    @Select("SELECT reserve_date AS date, COUNT(*) AS cnt "
            + "FROM reserve_order "
            + "WHERE reserve_date BETWEEN #{startDate} AND #{endDate} "
            + "AND (#{storeId} IS NULL OR store_id = #{storeId}) "
            + "GROUP BY reserve_date ORDER BY reserve_date")
    List<Map<String, Object>> reserveTrend(@Param("storeId") Long storeId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 周期内每日检测数
     */
    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS cnt "
            + "FROM vision_test_record "
            + "WHERE created_at >= #{startDate} AND created_at < #{endDatePlusOne} "
            + "AND (#{storeId} IS NULL OR store_id = #{storeId}) "
            + "GROUP BY DATE(created_at) ORDER BY DATE(created_at)")
    List<Map<String, Object>> testTrend(@Param("storeId") Long storeId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDatePlusOne") LocalDate endDatePlusOne);

    /**
     * 视力检测统计：按周聚合养护前/养护后平均视力
     */
    @Select("SELECT DATE(DATE_SUB(created_at, INTERVAL WEEKDAY(created_at) DAY)) AS week, "
            + "AVG(CASE WHEN test_type = 1 THEN vision_decimal END) AS beforeAvg, "
            + "AVG(CASE WHEN test_type = 2 THEN vision_decimal END) AS afterAvg "
            + "FROM vision_test_record "
            + "WHERE created_at >= #{startDate} AND created_at < #{endDatePlusOne} "
            + "AND (#{storeId} IS NULL OR store_id = #{storeId}) "
            + "GROUP BY week ORDER BY week")
    List<Map<String, Object>> visionWeekly(@Param("storeId") Long storeId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDatePlusOne") LocalDate endDatePlusOne);
}
