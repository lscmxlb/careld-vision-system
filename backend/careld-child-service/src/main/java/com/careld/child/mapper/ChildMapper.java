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
            "ORDER BY created_at DESC</script>")
    List<ChildProfile> selectByCondition(@Param("storeId") Long storeId,
                                          @Param("auditStatus") Integer auditStatus,
                                          @Param("parentUserId") Long parentUserId,
                                          @Param("keyword") String keyword);
    @Select("SELECT * FROM child_profile WHERE store_id = #{storeId} AND deleted_at IS NULL")
    List<ChildProfile> selectByStoreId(Long storeId);

    @Select("<script>SELECT COUNT(*) FROM child_profile WHERE deleted_at IS NULL " +
            "<if test='storeId != null'>AND store_id = #{storeId} </if>" +
            "<if test='parentUserId != null'>AND parent_user_id = #{parentUserId} </if>" +
            "AND audit_status = 0</script>")
    long countPending(@Param("storeId") Long storeId, @Param("parentUserId") Long parentUserId);

    /**
     * 聚合查询：JOIN store_info 取门店名、计算年龄、JOIN vision_test_record 取最近视力
     */
    String ENRICHED_COLUMNS =
            "c.id, c.child_code, c.store_id, c.name_encrypted, c.name_mask, c.phone_encrypted, c.phone_mask, " +
            "c.birth_date, c.gender, c.eye_condition, c.medical_history, c.allergy_info, c.family_history, " +
            "c.audit_status, c.audit_remark, c.audited_by, c.audited_at, c.parent_user_id, c.status, " +
            "c.created_by, c.updated_by, c.created_at, c.updated_at, c.deleted_at, " +
            "s.store_name AS store_name, " +
            "TIMESTAMPDIFF(YEAR, c.birth_date, CURDATE()) AS age, " +
            "vl.vision_level AS last_left_eye, " +
            "vr.vision_level AS last_right_eye, " +
            "COALESCE(vl.created_at, vr.created_at) AS last_test_time ";

    String ENRICHED_JOINS =
            "LEFT JOIN store_info s ON s.id = c.store_id AND s.deleted_at IS NULL " +
            "LEFT JOIN vision_test_record vl ON vl.child_id = c.id AND vl.eye_type = 1 " +
            "  AND vl.created_at = (SELECT MAX(created_at) FROM vision_test_record WHERE child_id = c.id AND eye_type = 1) " +
            "LEFT JOIN vision_test_record vr ON vr.child_id = c.id AND vr.eye_type = 2 " +
            "  AND vr.created_at = (SELECT MAX(created_at) FROM vision_test_record WHERE child_id = c.id AND eye_type = 2) ";

    @Select("<script>SELECT " + ENRICHED_COLUMNS +
            "FROM child_profile c " + ENRICHED_JOINS +
            "WHERE c.deleted_at IS NULL " +
            "<if test='storeId != null'>AND c.store_id = #{storeId} </if>" +
            "<if test='auditStatus != null'>AND c.audit_status = #{auditStatus} </if>" +
            "<if test='parentUserId != null'>AND c.parent_user_id = #{parentUserId} </if>" +
            "<if test='keyword != null'>AND (c.name_mask LIKE CONCAT('%',#{keyword},'%') OR c.phone_mask LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "ORDER BY c.created_at DESC</script>")
    List<ChildProfile> selectEnrichedList(@Param("storeId") Long storeId,
                                          @Param("auditStatus") Integer auditStatus,
                                          @Param("parentUserId") Long parentUserId,
                                          @Param("keyword") String keyword);

    @Select("SELECT " + ENRICHED_COLUMNS +
            "FROM child_profile c " + ENRICHED_JOINS +
            "WHERE c.id = #{id} AND c.deleted_at IS NULL")
    ChildProfile selectEnrichedById(@Param("id") Long id);
}
