package com.careld.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.user.dto.OperationLogResponse;
import com.careld.user.mapper.OperationLogMapper;
import com.careld.user.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志服务实现
 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public IPage<OperationLogResponse> pageLogs(Integer logType, String module, String keyword,
                                                LocalDate startDate, LocalDate endDate, Integer page, Integer size) {
        Page<OperationLogResponse> p = new Page<>(page == null ? 1 : page, size == null ? 20 : size);
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        return operationLogMapper.selectPageWithStore(p, logType, module, keyword, startTime, endTime);
    }

    @Override
    public OperationLogResponse getLogById(Long id) {
        OperationLogResponse log = operationLogMapper.selectDetailById(id);
        if (log == null) {
            throw new BusinessException(404, "日志不存在");
        }
        return log;
    }
}
