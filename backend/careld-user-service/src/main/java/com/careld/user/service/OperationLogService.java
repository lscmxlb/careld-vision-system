package com.careld.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.user.dto.OperationLogResponse;

import java.time.LocalDate;

/**
 * 操作日志服务
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志
     */
    IPage<OperationLogResponse> pageLogs(Integer logType, String module, String keyword,
                                         LocalDate startDate, LocalDate endDate, Integer page, Integer size);

    /**
     * 操作日志详情
     */
    OperationLogResponse getLogById(Long id);
}
