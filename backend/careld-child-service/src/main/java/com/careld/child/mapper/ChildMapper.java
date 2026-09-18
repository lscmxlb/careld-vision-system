package com.careld.child.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.child.entity.ChildProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
@Mapper
public interface ChildMapper extends BaseMapper<ChildProfile> {
    @Select("<script>SELECT * FROM child_profile WHERE deleted_at IS NULL " +
            "<if test='storeId != null'>AND store_id = #{storeId} </if>" +
            "<if test='auditStatus != null'>AND audit_status = #{auditStatus} </if>" +
            "<if test='parentUserId != null'>AND parent_user_id = #{parentUserId} </if>" +
            "<if test='keyword != null'>AND (name_mask LIKE CONCAT('%',#{keyword},'%') OR phone_mask LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='includeDisabled == null || includeDisabled == false'>AND status = 1 </if>" +
            "ORDER BY created_at DESC</script>")
    List<ChildProfile> selectByCondition(@Param("storeId") Long storeId,
                                          @Param("auditStatus") Integer auditStatus,
                                          @Param("parentUserId") Long parentUserId,
                                          @Param("keyword") String keyword,
                                          @Param("includeDisabled") Boolean includeDisabled);
    @Select("SELECT * FROM child_profile WHERE store_id = #{storeId} AND deleted_at IS NULL AND status = 1")
    List<ChildProfile> selectByStoreId(Long storeId);

    @Select("<script>SELECT COUNT(*) FROM child_profile WHERE deleted_at IS NULL " +
            "<if test='storeId != null'>AND store_id = #{storeId} </if>" +
            "<if test='parentUserId != null'>AND parent_user_id = #{parentUserId} </if>" +
            "AND audit_status = 0 AND status = 1</script>")
    long countPending(@Param("storeId") Long storeId, @Param("parentUserId") Long parentUserId);

    /**
     * 聚合查询：JOIN store_info 取门店名、计算年龄、JOIN vision_test_record 取最近视力
     */
    String ENRICHED_COLUMNS =
            "c.id, c.child_code, c.store_id, c.name_encrypted, c.name_mask, c.phone_encrypted, c.phone_mask, " +
            "c.birth_date, c.gender, c.parent_name, c.relation, " +
            "c.home_address, c.school, c.delivery_type, c.bedtime, c.wake_time, " +
            "c.eye_condition, c.naked_vision_both, c.naked_vision_left, c.naked_vision_right, " +
            "c.medical_history, c.allergy_info, c.family_history, " +
            "c.audit_status, c.audit_remark, c.audited_by, c.audited_at, c.parent_user_id, " +
            "c.doctor_id, c.doctor_name, c.source_type, c.source_user_id, c.status, c.remaining_count, " +
            "c.created_by, c.updated_by, c.created_at, c.updated_at, c.deleted_at, " +
            "s.store_name AS store_name, " +
            "TIMESTAMPDIFF(YEAR, c.birth_date, CURDATE()) AS age, " +
            // 养护次数（含养护中：status 1=养护中 2=已完成）
            "(SELECT COUNT(*) FROM care_record cr WHERE cr.child_id = c.id AND cr.status IN (1, 2) AND cr.deleted_at IS NULL) AS care_count, " +
            "vl.vision_level AS last_left_eye, " +
            "vr.vision_level AS last_right_eye, " +
            "COALESCE(vl.created_at, vr.created_at) AS last_test_time ";

    String ENRICHED_JOINS =
            "LEFT JOIN store_info s ON s.id = c.store_id AND s.deleted_at IS NULL " +
            // 最近视力检测：按 id 倒序取第一条，避免同一时间多条记录导致 JOIN 出重复行
            "LEFT JOIN vision_test_record vl ON vl.eye_type = 1 AND vl.id = " +
            "  (SELECT id FROM vision_test_record WHERE child_id = c.id AND eye_type = 1 ORDER BY created_at DESC, id DESC LIMIT 1) " +
            "LEFT JOIN vision_test_record vr ON vr.eye_type = 2 AND vr.id = " +
            "  (SELECT id FROM vision_test_record WHERE child_id = c.id AND eye_type = 2 ORDER BY created_at DESC, id DESC LIMIT 1) ";

    @Select("<script>SELECT " + ENRICHED_COLUMNS +
            "FROM child_profile c " + ENRICHED_JOINS +
            "WHERE c.deleted_at IS NULL " +
            "<if test='storeId != null'>AND c.store_id = #{storeId} </if>" +
            "<if test='auditStatus != null'>AND c.audit_status = #{auditStatus} </if>" +
            "<if test='parentUserId != null'>AND c.parent_user_id = #{parentUserId} </if>" +
            "<if test='keyword != null'>AND (c.name_mask LIKE CONCAT('%',#{keyword},'%') OR c.phone_mask LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'>AND c.status = #{status} </if>" +
            "<if test='status == null and (includeDisabled == null or includeDisabled == false)'>AND c.status = 1 </if>" +
            "ORDER BY c.created_at DESC</script>")
    List<ChildProfile> selectEnrichedList(@Param("storeId") Long storeId,
                                          @Param("auditStatus") Integer auditStatus,
                                          @Param("parentUserId") Long parentUserId,
                                          @Param("keyword") String keyword,
                                          @Param("includeDisabled") Boolean includeDisabled,
                                          @Param("status") Integer status);

    @Select("SELECT " + ENRICHED_COLUMNS +
            "FROM child_profile c " + ENRICHED_JOINS +
            "WHERE c.id = #{id} AND c.deleted_at IS NULL")
    ChildProfile selectEnrichedById(@Param("id") Long id);
}
