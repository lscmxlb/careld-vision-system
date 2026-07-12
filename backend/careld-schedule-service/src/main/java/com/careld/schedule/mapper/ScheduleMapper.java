package com.careld.schedule.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.schedule.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDate;
import java.util.List;
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
    @Select("SELECT * FROM schedule_info WHERE store_id = #{storeId} AND schedule_date BETWEEN #{startDate} AND #{endDate} AND deleted_at IS NULL")
    List<Schedule> selectByDateRange(@Param("storeId") Long storeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    @Update("UPDATE schedule_info SET reserved_count = reserved_count + 1 WHERE id = #{id} AND reserved_count < max_capacity")
    int incrementReserved(@Param("id") Long id);

    @Update("UPDATE schedule_info SET reserved_count = GREATEST(reserved_count - 1, 0) WHERE id = #{id}")
    int decrementReserved(@Param("id") Long id);
}
