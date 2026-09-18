package com.careld.vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.vision.entity.CareRecord;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CareRecordMapper extends BaseMapper<CareRecord> {

    /** 儿童姓名搜索候选（掩码 + 密文，供 service 层解密后模糊匹配全名） */
    @Data
    class ChildNameCandidate {
        private Long id;
        private String nameMask;
        private String nameEncrypted;
    }

    /** 养护时段计算来源：预约单实际录入的开始养护时间与结束养护时间 */
    @Data
    class CarePeriodSource {
        private Long appointmentId;
        private LocalDateTime startTime;
        private LocalDateTime completedAt;
    }

    @Select("<script>" +
            "SELECT id AS appointmentId, start_time, completed_at FROM reserve_order " +
            "WHERE id IN <foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach> " +
            "</script>")
    List<CarePeriodSource> selectCarePeriodSources(@Param("ids") List<Long> ids);

    @Select("<script>" +
            "SELECT id, name_mask, name_encrypted FROM child_profile " +
            "WHERE deleted_at IS NULL " +
            "<if test='storeId != null'>AND store_id = #{storeId} </if>" +
            "</script>")
    List<ChildNameCandidate> selectChildNameCandidates(@Param("storeId") Long storeId);

    /**
     * 养护记录分页查询：JOIN child_profile 支持按儿童姓名/家长姓名/手机号筛选，
     * 并支持按儿童累计已完成养护次数（大于 x 次）筛选
     */
    @Select("<script>" +
            "SELECT r.*, " +
            "c.name_mask AS childName, " +
            "c.parent_name AS parentName, " +
            "c.phone_mask AS parentPhone, " +
            "c.phone_mask AS childPhone, " +
            "c.gender AS childGender, " +
            "c.remaining_count AS remainingCount, " +
            "c.naked_vision_both AS nakedVisionBoth, " +
            "c.name_encrypted AS childNameEncrypted, " +
            "(SELECT COUNT(*) FROM care_record cr3 WHERE cr3.child_id = r.child_id AND cr3.deleted_at IS NULL AND cr3.status = 2) AS careCount, " +
            "o.remark AS remark " +
            "FROM care_record r " +
            "LEFT JOIN child_profile c ON c.id = r.child_id AND c.deleted_at IS NULL " +
            "LEFT JOIN reserve_order o ON o.id = r.appointment_id AND o.deleted_at IS NULL " +
            "WHERE r.deleted_at IS NULL " +
            "<if test='storeId != null'>AND r.store_id = #{storeId} </if>" +
            "<if test='childId != null'>AND r.child_id = #{childId} </if>" +
            "<if test='childIds != null'>AND r.child_id IN <foreach collection='childIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach> </if>" +
            "<if test='parentName != null and parentName != \"\"'>AND c.parent_name LIKE CONCAT('%',#{parentName},'%') </if>" +
            "<if test='phone != null and phone != \"\"'>" +
            "<choose>" +
            // 完整 11 位手机号：按脱敏格式（前3+****+后4）精确匹配
            "<when test='phone.length() == 11'>AND c.phone_mask = CONCAT(LEFT(#{phone},3),'****',RIGHT(#{phone},4)) </when>" +
            // 非完整号码：按脱敏号码模糊匹配（支持输前3位或后4位）
            "<otherwise>AND c.phone_mask LIKE CONCAT('%',#{phone},'%') </otherwise>" +
            "</choose>" +
            "</if>" +
            "<if test='minCareCount != null'>AND r.child_id IN (" +
            "SELECT cr.child_id FROM care_record cr WHERE cr.deleted_at IS NULL AND cr.status = 2 " +
            "<if test='storeId != null'>AND cr.store_id = #{storeId} </if>" +
            "GROUP BY cr.child_id HAVING COUNT(*) &gt; #{minCareCount}) </if>" +
            "ORDER BY r.care_date DESC, r.id DESC</script>")
    IPage<CareRecord> selectPageWithChild(Page<CareRecord> page,
                                          @Param("storeId") Long storeId,
                                          @Param("childId") Long childId,
                                          @Param("childIds") List<Long> childIds,
                                          @Param("parentName") String parentName,
                                          @Param("phone") String phone,
                                          @Param("minCareCount") Integer minCareCount);

    /** 养护记录详情：聚合字段口径与分页查询保持一致 */
    @Select("SELECT r.*, " +
            "c.name_mask AS childName, " +
            "c.parent_name AS parentName, " +
            "c.phone_mask AS parentPhone, " +
            "c.phone_mask AS childPhone, " +
            "c.gender AS childGender, " +
            "c.remaining_count AS remainingCount, " +
            "c.naked_vision_both AS nakedVisionBoth, " +
            "c.name_encrypted AS childNameEncrypted, " +
            "(SELECT COUNT(*) FROM care_record cr3 WHERE cr3.child_id = r.child_id AND cr3.deleted_at IS NULL AND cr3.status = 2) AS careCount, " +
            "o.remark AS remark " +
            "FROM care_record r " +
            "LEFT JOIN child_profile c ON c.id = r.child_id AND c.deleted_at IS NULL " +
            "LEFT JOIN reserve_order o ON o.id = r.appointment_id AND o.deleted_at IS NULL " +
            "WHERE r.deleted_at IS NULL AND r.id = #{id}")
    CareRecord selectDetailById(@Param("id") Long id);
}
