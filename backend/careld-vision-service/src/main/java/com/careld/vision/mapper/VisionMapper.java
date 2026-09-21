package com.careld.vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.vision.dto.VisionQuery;
import com.careld.vision.dto.VisionRecordGroup;
import com.careld.vision.entity.VisionTestRecord;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VisionMapper extends BaseMapper<VisionTestRecord> {

    /** 儿童姓名搜索候选（掩码 + 密文，供 service 层解密后模糊匹配全名） */
    @Data
    class ChildNameCandidate {
        private Long id;
        private String nameMask;
        private String nameEncrypted;
    }

    /** 列表与配对查询共用的过滤条件（表别名 v） */
    String FILTERS =
            "<if test='effectiveStoreId != null'>AND v.store_id = #{effectiveStoreId} </if>" +
            "<if test='childId != null'>AND v.child_id = #{childId} </if>" +
            "<if test='childIds != null'>AND v.child_id IN " +
            "<foreach collection='childIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach> </if>" +
            "<if test='testType != null'>AND v.test_type = #{testType} </if>" +
            "<if test='eyeType != null'>AND v.eye_type = #{eyeType} </if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "<choose>" +
            "<when test='keywordChildIds != null'>AND (v.record_code LIKE CONCAT('%',#{keyword},'%') OR v.child_id IN " +
            "<foreach collection='keywordChildIds' item='kid' open='(' separator=',' close=')'>#{kid}</foreach>) </when>" +
            "<otherwise>AND v.record_code LIKE CONCAT('%',#{keyword},'%') </otherwise>" +
            "</choose>" +
            "</if>" +
            "<if test='startTime != null'>AND v.created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'>AND v.created_at &lt;= #{endTime} </if>";

    /** 一次检测事件的左右眼两行按「儿童 + 预约 + 检测阶段 + 检测时间（精确到秒）」归并 */
    String GROUP_KEY = "v.child_id, IFNULL(v.reserve_id, 0), v.test_type, DATE_FORMAT(v.created_at, '%Y-%m-%d %H:%i:%s')";

    String EYES =
            "MAX(CASE WHEN v.eye_type = 1 OR v.eye_type = 0 OR v.eye_type IS NULL THEN v.vision_level END) ";

    @Select("<script>" +
            "SELECT COUNT(*) FROM vision_test_record v WHERE v.deleted_at IS NULL " + FILTERS +
            "</script>")
    long countByQuery(VisionQuery query);

    @Select("<script>" +
            "SELECT v.*, c.name_mask AS childName, c.name_encrypted AS childNameEncrypted, " +
            "s.store_name AS storeName, v.created_at AS testTime " +
            "FROM vision_test_record v " +
            "LEFT JOIN child_profile c ON c.id = v.child_id AND c.deleted_at IS NULL " +
            "LEFT JOIN store_info s ON s.id = v.store_id AND s.deleted_at IS NULL " +
            "WHERE v.deleted_at IS NULL " + FILTERS +
            "ORDER BY v.created_at DESC, v.id DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<VisionTestRecord> selectPageByQuery(VisionQuery query);

    @Select("SELECT v.*, c.name_mask AS childName, c.name_encrypted AS childNameEncrypted, " +
            "s.store_name AS storeName, v.created_at AS testTime " +
            "FROM vision_test_record v " +
            "LEFT JOIN child_profile c ON c.id = v.child_id AND c.deleted_at IS NULL " +
            "LEFT JOIN store_info s ON s.id = v.store_id AND s.deleted_at IS NULL " +
            "WHERE v.deleted_at IS NULL AND v.id = #{id}")
    VisionTestRecord selectDetailById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT COUNT(*) FROM (SELECT 1 FROM vision_test_record v WHERE v.deleted_at IS NULL " + FILTERS +
            " GROUP BY " + GROUP_KEY + ") t" +
            "</script>")
    long countGroupedByQuery(VisionQuery query);

    @Select("<script>" +
            "SELECT v.child_id AS childId, MAX(c.name_mask) AS childName, " +
            "MAX(c.name_encrypted) AS childNameEncrypted, MAX(s.store_name) AS storeName, " +
            "MAX(v.reserve_id) AS reserveId, v.test_type AS testType, MIN(v.created_at) AS testTime, " +
            EYES + "AS leftEye, " +
            "MAX(CASE WHEN v.eye_type = 2 OR v.eye_type = 0 OR v.eye_type IS NULL THEN v.vision_level END) AS rightEye, " +
            "MAX(v.tester_name) AS testerName, MAX(v.remark) AS remark " +
            "FROM vision_test_record v " +
            "LEFT JOIN child_profile c ON c.id = v.child_id AND c.deleted_at IS NULL " +
            "LEFT JOIN store_info s ON s.id = v.store_id AND s.deleted_at IS NULL " +
            "WHERE v.deleted_at IS NULL " + FILTERS +
            "GROUP BY " + GROUP_KEY + " " +
            "ORDER BY testTime DESC, childId ASC, testType ASC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<VisionRecordGroup> selectGroupedPage(VisionQuery query);

    /** 改善情况对比基准：指定儿童的全部检测事件（仅取左右眼视力与检测时间） */
    @Select("<script>" +
            "SELECT v.child_id AS childId, v.test_type AS testType, MIN(v.created_at) AS testTime, " +
            EYES + "AS leftEye, " +
            "MAX(CASE WHEN v.eye_type = 2 OR v.eye_type = 0 OR v.eye_type IS NULL THEN v.vision_level END) AS rightEye " +
            "FROM vision_test_record v WHERE v.deleted_at IS NULL AND v.child_id IN " +
            "<foreach collection='childIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach> " +
            "GROUP BY " + GROUP_KEY +
            "</script>")
    List<VisionRecordGroup> selectGroupedByChildIds(@Param("childIds") List<Long> childIds);

    @Select("<script>SELECT id, name_mask, name_encrypted FROM child_profile " +
            "WHERE deleted_at IS NULL " +
            "<if test='storeId != null'>AND store_id = #{storeId} </if>" +
            "</script>")
    List<ChildNameCandidate> selectChildNameCandidates(@Param("storeId") Long storeId);

    @Select("SELECT * FROM vision_test_record WHERE child_id = #{childId} AND deleted_at IS NULL ORDER BY created_at DESC")
    List<VisionTestRecord> selectByChildId(Long childId);

    @Select("SELECT * FROM vision_test_record WHERE child_id = #{childId} AND reserve_id = #{reserveId} AND deleted_at IS NULL")
    List<VisionTestRecord> selectByChildAndReserve(@Param("childId") Long childId, @Param("reserveId") Long reserveId);
}
