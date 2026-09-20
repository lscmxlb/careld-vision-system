package com.careld.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.schedule.entity.ReserveOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReserveOrderMapper extends BaseMapper<ReserveOrder> {

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
}
