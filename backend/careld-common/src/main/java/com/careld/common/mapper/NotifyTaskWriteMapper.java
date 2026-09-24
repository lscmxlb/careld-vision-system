package com.careld.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.common.entity.NotifyTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知任务写入 Mapper
 *
 * <p>读侧（待发送任务轮询、状态流转）在 careld-notify-service 的 NotifyTaskMapper 中。</p>
 */
@Mapper
public interface NotifyTaskWriteMapper extends BaseMapper<NotifyTask> {
}
