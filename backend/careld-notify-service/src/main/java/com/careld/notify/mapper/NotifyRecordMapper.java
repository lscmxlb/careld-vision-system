package com.careld.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.notify.dto.NotifyQuery;
import com.careld.notify.entity.NotifyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface NotifyRecordMapper extends BaseMapper<NotifyRecord> {

    String FILTERS =
            "<if test='storeId != null'>AND store_id = #{storeId} </if>" +
            "<if test='eventType != null and eventType != \"\"'>AND event_type = #{eventType} </if>" +
            "<if test='channel != null'>AND channel = #{channel} </if>" +
            "<if test='status != null'>AND status = #{status} </if>" +
            "<if test='startTime != null'>AND sent_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'>AND sent_at &lt;= #{endTime} </if>" +
            // 姓名检索：命中「解密后全名/掩码片段」的儿童ID，或直接匹配记录中的脱敏姓名快照
            "<if test='childNameLike != null and childNameLike != \"\"'>" +
            "AND (<if test='childIds != null'>child_id IN " +
            "<foreach collection='childIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach> OR </if>" +
            "child_name LIKE CONCAT('%', #{childNameLike}, '%')) </if>";

    @Select("<script>SELECT COUNT(*) FROM notify_record WHERE deleted_at IS NULL " + FILTERS + "</script>")
    long countByQuery(NotifyQuery query);

    @Select("<script>SELECT * FROM notify_record WHERE deleted_at IS NULL " + FILTERS
            + " ORDER BY sent_at DESC, id DESC LIMIT #{offset}, #{size}</script>")
    List<NotifyRecord> selectPageByQuery(NotifyQuery query);

    @Select("<script>SELECT IFNULL(SUM(fee), 0) FROM notify_record WHERE deleted_at IS NULL " + FILTERS + "</script>")
    BigDecimal sumFeeByQuery(NotifyQuery query);

    @Select("SELECT IFNULL(SUM(fee), 0) FROM notify_record WHERE deleted_at IS NULL AND store_id = #{storeId}")
    BigDecimal sumFeeByStore(@Param("storeId") Long storeId);

    @Select("SELECT COUNT(*) FROM notify_record WHERE task_id = #{taskId} AND deleted_at IS NULL")
    long countByTask(@Param("taskId") Long taskId);
}
