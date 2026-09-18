package com.careld.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.schedule.entity.CareRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CareRecordMapper extends BaseMapper<CareRecord> {

    @Select("SELECT * FROM care_record WHERE appointment_id = #{appointmentId} AND deleted_at IS NULL LIMIT 1")
    CareRecord selectByAppointmentId(@Param("appointmentId") Long appointmentId);

    @Update("UPDATE care_record SET vision_after_left = #{afterLeft}, vision_after_right = #{afterRight}, "
            + "vision_after_both = #{afterBoth}, status = 2, updated_at = NOW() WHERE appointment_id = #{appointmentId}")
    int completeCare(@Param("appointmentId") Long appointmentId,
                     @Param("afterLeft") String afterLeft,
                     @Param("afterRight") String afterRight,
                     @Param("afterBoth") String afterBoth);
}
