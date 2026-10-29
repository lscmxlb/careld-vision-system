package com.careld.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 医务人员删除时同步各业务表中冗余的姓名快照（同库跨表更新）。
 * 历史记录部分行只有姓名没有 id，按「门店 + 原姓名」兜底匹配。
 */
@Mapper
public interface MedicalStaffRecordMapper {

    @Update("UPDATE child_service_record SET doctor_name = CONCAT(LEFT(doctor_name, 44), '[已经删除]') "
            + "WHERE doctor_name IS NOT NULL AND doctor_name NOT LIKE '%[已经删除]' "
            + "AND (doctor_id = #{id} OR (doctor_id IS NULL AND store_id = #{storeId} AND doctor_name = #{name}))")
    int markChildServiceRecord(@Param("id") Long id, @Param("storeId") Long storeId, @Param("name") String name);

    @Update("UPDATE child_profile SET doctor_name = CONCAT(LEFT(doctor_name, 44), '[已经删除]') "
            + "WHERE doctor_name IS NOT NULL AND doctor_name NOT LIKE '%[已经删除]' "
            + "AND (doctor_id = #{id} OR (doctor_id IS NULL AND store_id = #{storeId} AND doctor_name = #{name}))")
    int markChildProfile(@Param("id") Long id, @Param("storeId") Long storeId, @Param("name") String name);

    @Update("UPDATE care_record SET executor_name = CONCAT(LEFT(executor_name, 58), '[已经删除]') "
            + "WHERE executor_name IS NOT NULL AND executor_name NOT LIKE '%[已经删除]' "
            + "AND (executor_id = #{id} OR (executor_id IS NULL AND store_id = #{storeId} AND executor_name = #{name}))")
    int markCareRecord(@Param("id") Long id, @Param("storeId") Long storeId, @Param("name") String name);

    @Update("UPDATE reserve_order SET executor_name = CONCAT(LEFT(executor_name, 58), '[已经删除]') "
            + "WHERE executor_name IS NOT NULL AND executor_name NOT LIKE '%[已经删除]' "
            + "AND (executor_id = #{id} OR (executor_id IS NULL AND store_id = #{storeId} AND executor_name = #{name}))")
    int markReserveOrder(@Param("id") Long id, @Param("storeId") Long storeId, @Param("name") String name);
}
