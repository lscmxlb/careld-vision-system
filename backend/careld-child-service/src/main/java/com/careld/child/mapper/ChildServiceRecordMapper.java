package com.careld.child.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.child.entity.ChildServiceRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChildServiceRecordMapper extends BaseMapper<ChildServiceRecord> {

    /**
     * 剩余次数增减（由调用方保证事务与先后校验）
     */
    @Update("UPDATE child_profile SET remaining_count = remaining_count + #{delta} WHERE id = #{childId}")
    int changeRemaining(@Param("childId") Long childId, @Param("delta") int delta);

    /**
     * 写入次数变更流水
     */
    @Insert("INSERT INTO child_service_record (child_id, store_id, change_type, change_count, appointment_id, payment_amount, payment_method, doctor_id, doctor_name, operator_id, remark, created_at, updated_at) "
            + "VALUES (#{r.childId}, #{r.storeId}, #{r.changeType}, #{r.changeCount}, #{r.appointmentId}, #{r.paymentAmount}, #{r.paymentMethod}, #{r.doctorId}, #{r.doctorName}, #{r.operatorId}, #{r.remark}, NOW(), NOW())")
    int insertRecord(@Param("r") ChildServiceRecord record);
}
