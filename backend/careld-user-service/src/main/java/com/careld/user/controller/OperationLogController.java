package com.careld.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.user.dto.OperationLogResponse;
import com.careld.user.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 操作日志控制器
 */
@Tag(name = "操作日志管理")
@RestController
@RequestMapping("/api/v1/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "操作日志分页列表")
    @GetMapping
    public Result<PageResult<OperationLogResponse>> list(
            @RequestParam(value = "logType", required = false) Integer logType,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        IPage<OperationLogResponse> p = operationLogService.pageLogs(logType, module, keyword, startDate, endDate, page, size);
        return Result.success(PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal()));
    }

    @Operation(summary = "操作日志详情")
    @GetMapping("/{id}")
    public Result<OperationLogResponse> get(@PathVariable Long id) {
        return Result.success(operationLogService.getLogById(id));
    }
}
