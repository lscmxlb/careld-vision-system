package com.careld.child.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.child.entity.TrialCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface TrialCardMapper extends BaseMapper<TrialCard> {

    String ENRICHED_COLUMNS =
            "t.*, oc.center_name AS center_name, ag.agent_name AS agent_name, " +
            "st.store_name AS store_name, ust.store_name AS used_store_name, ust.store_type AS used_store_type, " +
            "cp.name_encrypted AS child_name_encrypted, cp.phone_encrypted AS parent_phone_encrypted, " +
            "cp.parent_name AS parent_name, " +
            "COALESCE(NULLIF(u.real_name, ''), u.username) AS creator_name ";

    String ENRICHED_JOINS =
            "FROM trial_card t " +
            "LEFT JOIN ops_center oc ON oc.id = t.center_id AND oc.deleted_at IS NULL " +
            "LEFT JOIN agent ag ON ag.id = t.agent_id AND ag.deleted_at IS NULL " +
            "LEFT JOIN store_info st ON st.id = t.store_id AND st.deleted_at IS NULL " +
            "LEFT JOIN store_info ust ON ust.id = t.used_store_id AND ust.deleted_at IS NULL " +
            "LEFT JOIN child_profile cp ON cp.id = t.used_child_id " +
            "LEFT JOIN sys_user u ON u.id = t.created_by ";

    String ENRICHED_WHERE =
            "WHERE t.deleted_at IS NULL " +
            "<if test='status != null'>AND t.status = #{status} </if>" +
            "<if test='centerId != null'>AND t.center_id = #{centerId} </if>" +
            "<if test='agentId != null'>AND t.agent_id = #{agentId} </if>" +
            "<if test='storeId != null'>AND t.store_id = #{storeId} </if>" +
            "<if test='keyword != null'>AND t.card_no LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "<if test='startDate != null'>AND t.created_at &gt;= CONCAT(#{startDate}, ' 00:00:00') </if>" +
            "<if test='endDate != null'>AND t.created_at &lt;= CONCAT(#{endDate}, ' 23:59:59') </if>";

    @Select("<script>SELECT " + ENRICHED_COLUMNS + ENRICHED_JOINS + ENRICHED_WHERE +
            "ORDER BY t.id DESC LIMIT #{offset}, #{size}</script>")
    List<TrialCard> selectEnrichedPage(@Param("status") Integer status,
                                       @Param("centerId") Long centerId,
                                       @Param("agentId") Long agentId,
                                       @Param("storeId") Long storeId,
                                       @Param("keyword") String keyword,
                                       @Param("startDate") String startDate,
                                       @Param("endDate") String endDate,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    @Select("<script>SELECT COUNT(*) " + ENRICHED_JOINS + ENRICHED_WHERE + "</script>")
    long countEnriched(@Param("status") Integer status,
                       @Param("centerId") Long centerId,
                       @Param("agentId") Long agentId,
                       @Param("storeId") Long storeId,
                       @Param("keyword") String keyword,
                       @Param("startDate") String startDate,
                       @Param("endDate") String endDate);

    /** 状态分组统计（与列表同筛选口径；status 传 null 时不按状态过滤，供统计栏使用） */
    @Select("<script>SELECT t.status AS status, COUNT(*) AS cnt FROM trial_card t " + ENRICHED_WHERE + "GROUP BY t.status</script>")
    List<Map<String, Object>> countGroupByStatus(@Param("status") Integer status,
                                                 @Param("centerId") Long centerId,
                                                 @Param("agentId") Long agentId,
                                                 @Param("storeId") Long storeId,
                                                 @Param("keyword") String keyword,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);

    /** 同前缀（年份+区号）内已用的最大顺序号；软删记录一并计入，避免编号复用 */
    @Select("SELECT COALESCE(MAX(seq_no), 0) FROM trial_card WHERE card_no LIKE CONCAT(#{prefix}, '%')")
    int selectMaxSeq(@Param("prefix") String prefix);

    @Select("SELECT * FROM trial_card WHERE card_no = #{cardNo} AND deleted_at IS NULL")
    TrialCard selectByCardNo(@Param("cardNo") String cardNo);

    /** 兑换绑定：0→2，写入绑定时间（status=0 为并发前置条件，重复兑换时影响行数为 0） */
    @Update("UPDATE trial_card SET status = 2, bound_at = NOW(), used_store_id = #{storeId}, used_child_id = #{childId}, "
            + "used_parent_user_id = #{parentUserId}, updated_by = #{operatorId}, updated_at = NOW() "
            + "WHERE id = #{id} AND status = 0 AND deleted_at IS NULL")
    int markRedeemed(@Param("id") Long id, @Param("storeId") Long storeId, @Param("childId") Long childId,
                     @Param("parentUserId") Long parentUserId, @Param("operatorId") Long operatorId);

    /** 禁用：0→3（仅未兑换可禁用，status=0 为并发前置条件） */
    @Update("UPDATE trial_card SET status = 3, updated_by = #{operatorId}, updated_at = NOW() "
            + "WHERE id = #{id} AND status = 0 AND deleted_at IS NULL")
    int markDisabled(@Param("id") Long id, @Param("operatorId") Long operatorId);

    /** 启用：3→0，恢复为未兑换（status=3 为并发前置条件） */
    @Update("UPDATE trial_card SET status = 0, updated_by = #{operatorId}, updated_at = NOW() "
            + "WHERE id = #{id} AND status = 3 AND deleted_at IS NULL")
    int markEnabled(@Param("id") Long id, @Param("operatorId") Long operatorId);

    /** 区间批量禁用：仅命中未兑换（status=0），返回实际禁用行数 */
    @Update("UPDATE trial_card SET status = 3, updated_by = #{operatorId}, updated_at = NOW() "
            + "WHERE card_no BETWEEN #{startCardNo} AND #{endCardNo} AND status = 0 AND deleted_at IS NULL")
    int disableRange(@Param("startCardNo") String startCardNo, @Param("endCardNo") String endCardNo,
                     @Param("operatorId") Long operatorId);

    /** 区间内有效卡总数（含各状态，用于批量禁用的跳过统计） */
    @Select("SELECT COUNT(*) FROM trial_card WHERE card_no BETWEEN #{startCardNo} AND #{endCardNo} AND deleted_at IS NULL")
    long countInRange(@Param("startCardNo") String startCardNo, @Param("endCardNo") String endCardNo);

    @Select("SELECT COUNT(*) FROM ops_center WHERE id = #{centerId} AND deleted_at IS NULL")
    int countCenter(@Param("centerId") Long centerId);

    @Select("SELECT COUNT(*) FROM agent WHERE id = #{agentId} AND center_id = #{centerId} AND deleted_at IS NULL")
    int countAgentInCenter(@Param("agentId") Long agentId, @Param("centerId") Long centerId);
}
