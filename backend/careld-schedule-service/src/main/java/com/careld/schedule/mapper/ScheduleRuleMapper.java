package com.careld.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.schedule.entity.ScheduleRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleRuleMapper extends BaseMapper<ScheduleRule> {

    /**
     * 查询与区间 [startDate, endDate] 存在交集的启用规则（用于同一日期唯一性校验）
     */
    @Select("SELECT * FROM schedule_rule WHERE store_id = #{storeId} AND deleted_at IS NULL "
            + "AND status = 1 AND start_date <= #{endDate} AND end_date >= #{startDate}")
    List<ScheduleRule> selectOverlapping(@Param("storeId") Long storeId,
                                         @Param("startDate") java.time.LocalDate startDate,
                                         @Param("endDate") java.time.LocalDate endDate);

    @Select("SELECT * FROM schedule_rule WHERE store_id = #{storeId} AND deleted_at IS NULL ORDER BY start_date DESC")
    List<ScheduleRule> selectByStore(@Param("storeId") Long storeId);
}
