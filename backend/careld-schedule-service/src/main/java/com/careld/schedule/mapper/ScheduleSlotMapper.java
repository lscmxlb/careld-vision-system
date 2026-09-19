package com.careld.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.schedule.entity.ScheduleSlot;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ScheduleSlotMapper extends BaseMapper<ScheduleSlot> {

    @Select("SELECT * FROM schedule_slot WHERE store_id = #{storeId} AND slot_date = #{date} AND status = 1 ORDER BY slot_start_time")
    List<ScheduleSlot> selectByDate(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM schedule_slot WHERE store_id = #{storeId} AND slot_date BETWEEN #{startDate} AND #{endDate} AND booked_count > 0")
    long countBookedInRange(@Param("storeId") Long storeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM schedule_slot WHERE store_id = #{storeId} AND slot_date BETWEEN #{startDate} AND #{endDate} AND booked_count > 0")
    List<ScheduleSlot> selectBookedInRange(@Param("storeId") Long storeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 按天聚合开放时段名额：booked=Σ已约、total=Σ时段容量（总量固定，不随已约变化；无排班日期不返回）
     */
    @Select("SELECT slot_date AS slotDate, SUM(booked_count) AS booked, SUM(max_capacity) AS total " +
            "FROM schedule_slot WHERE store_id = #{storeId} AND slot_date BETWEEN #{startDate} AND #{endDate} AND status = 1 " +
            "GROUP BY slot_date ORDER BY slot_date")
    List<Map<String, Object>> sumDailyByRange(@Param("storeId") Long storeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 预约占位（带容量保护的原子自增，返回 0 表示已满）
     */
    @Update("UPDATE schedule_slot SET booked_count = booked_count + 1 WHERE id = #{id} AND status = 1 AND booked_count < max_capacity")
    int incrementBooked(@Param("id") Long id);

    @Update("UPDATE schedule_slot SET booked_count = GREATEST(booked_count - 1, 0) WHERE id = #{id}")
    int decrementBooked(@Param("id") Long id);

    /** 仅同步容量（不触碰 booked_count，避免并发预约被旧值覆盖） */
    @Update("UPDATE schedule_slot SET max_capacity = #{capacity} WHERE id = #{id}")
    int updateCapacity(@Param("id") Long id, @Param("capacity") Integer capacity);

    @Delete("DELETE FROM schedule_slot WHERE store_id = #{storeId} AND slot_date BETWEEN #{startDate} AND #{endDate} AND booked_count = 0")
    int deleteUnbookedInRange(@Param("storeId") Long storeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
