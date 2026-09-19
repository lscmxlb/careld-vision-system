package com.careld.schedule.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 儿童剩余次数与流水（共享库直写，保证与预约状态变更同事务）
 */
@Mapper
public interface QuotaMapper {

    @Select("SELECT remaining_count FROM child_profile WHERE id = #{childId} AND deleted_at IS NULL")
    Integer selectRemaining(@Param("childId") Long childId);

    @Select("SELECT audit_status FROM child_profile WHERE id = #{childId} AND deleted_at IS NULL")
    Integer selectAuditStatus(@Param("childId") Long childId);

    @Select("SELECT status FROM child_profile WHERE id = #{childId} AND deleted_at IS NULL")
    Integer selectChildStatus(@Param("childId") Long childId);

    @Select("SELECT store_id FROM child_profile WHERE id = #{childId} AND deleted_at IS NULL")
    Long selectChildStoreId(@Param("childId") Long childId);

    @Select("SELECT name_mask AS nameMask, phone_mask AS phoneMask, parent_name AS parentName "
            + "FROM child_profile WHERE id = #{childId} AND deleted_at IS NULL")
    java.util.Map<String, String> selectChildMask(@Param("childId") Long childId);

    @Select("SELECT name_encrypted FROM child_profile WHERE id = #{childId} AND deleted_at IS NULL")
    String selectChildNameEncrypted(@Param("childId") Long childId);

    @Select("SELECT id, name_encrypted AS nameEncrypted, name_mask AS nameMask, "
            + "phone_encrypted AS phoneEncrypted, phone_mask AS phoneMask "
            + "FROM child_profile WHERE deleted_at IS NULL")
    java.util.List<java.util.Map<String, Object>> selectChildSearchFields();

    @Select("SELECT COUNT(*) FROM reserve_order WHERE child_id = #{childId} "
            + "AND reserve_date = #{date} AND status IN (1, 2) AND deleted_at IS NULL")
    long countActiveByChildAndDate(@Param("childId") Long childId, @Param("date") java.time.LocalDate date);

    @Update("UPDATE child_profile SET remaining_count = remaining_count + #{delta} WHERE id = #{childId}")
    int changeRemaining(@Param("childId") Long childId, @Param("delta") int delta);

    @Insert("INSERT INTO child_service_record (child_id, store_id, change_type, change_count, appointment_id, operator_id, remark, created_at, updated_at) "
            + "VALUES (#{childId}, #{storeId}, #{changeType}, #{changeCount}, #{appointmentId}, #{operatorId}, #{remark}, NOW(), NOW())")
    int insertQuotaRecord(@Param("childId") Long childId, @Param("storeId") Long storeId,
                          @Param("changeType") int changeType, @Param("changeCount") int changeCount,
                          @Param("appointmentId") Long appointmentId, @Param("operatorId") Long operatorId,
                          @Param("remark") String remark);
}
