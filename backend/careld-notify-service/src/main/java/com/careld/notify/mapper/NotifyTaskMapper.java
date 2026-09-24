package com.careld.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.common.entity.NotifyTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NotifyTaskMapper extends BaseMapper<NotifyTask> {

    @Select("SELECT * FROM notify_task WHERE status = 0 AND deleted_at IS NULL ORDER BY id ASC LIMIT #{limit}")
    List<NotifyTask> selectPending(@Param("limit") int limit);

    /** 抢占任务：仅当仍为待发送时改为处理中，避免重复消费 */
    @Update("UPDATE notify_task SET status = 9, updated_at = NOW() WHERE id = #{id} AND status = 0")
    int claim(@Param("id") Long id);

    @Update("UPDATE notify_task SET status = #{status}, retry_count = #{retryCount}, remark = #{remark}, "
            + "updated_at = NOW() WHERE id = #{id}")
    int finish(@Param("id") Long id, @Param("status") int status,
               @Param("retryCount") int retryCount, @Param("remark") String remark);

    /** 异常重试：回到待发送并累加重试次数 */
    @Update("UPDATE notify_task SET status = 0, retry_count = #{retryCount}, remark = #{remark}, "
            + "updated_at = NOW() WHERE id = #{id}")
    int requeue(@Param("id") Long id, @Param("retryCount") int retryCount, @Param("remark") String remark);

    /** 回收卡在「处理中」的僵尸任务（服务重启/中途崩溃），避免任务永久滞留 */
    @Update("UPDATE notify_task SET status = 0, remark = '处理超时，重新排队', updated_at = NOW() "
            + "WHERE status = 9 AND updated_at < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) AND deleted_at IS NULL")
    int requeueStale(@Param("minutes") int minutes);
}
