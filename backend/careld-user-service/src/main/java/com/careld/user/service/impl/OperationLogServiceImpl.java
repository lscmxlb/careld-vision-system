package com.careld.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.common.security.DataScopeHelper;
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
    public IPage<OperationLogResponse> pageLogs(Long storeId, Integer logType, String module, String action, String userName,
                                                String keyword, LocalDate startDate, LocalDate endDate,
                                                Integer page, Integer size) {
        Page<OperationLogResponse> p = new Page<>(page == null ? 1 : page, size == null ? 20 : size);
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        // 门店用户忽略传入值强制只看本门店日志；总部/超管可按医院筛选
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        return operationLogMapper.selectPageWithStore(p, effectiveStoreId, logType, module, action, userName,
                keyword, startTime, endTime);
    }

    @Override
    public OperationLogResponse getLogById(Long id) {
        OperationLogResponse log = operationLogMapper.selectDetailById(id, DataScopeHelper.resolveStoreId(null));
        if (log == null) {
            throw new BusinessException(404, "日志不存在或无权查看");
        }
        return log;
    }
}
