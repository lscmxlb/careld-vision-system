package com.careld.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.common.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志写入 Mapper
 *
 * <p>读侧查询（分页/详情）在 careld-user-service 的 OperationLogMapper 中。</p>
 */
@Mapper
public interface OperationLogWriteMapper extends BaseMapper<SysOperationLog> {
}
