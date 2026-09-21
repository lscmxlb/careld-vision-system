package com.careld.vision.controller;

import com.careld.common.log.OperationLog;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.vision.dto.VisionQuery;
import com.careld.vision.dto.VisionRecordGroup;
import com.careld.vision.entity.VisionTestRecord;
import com.careld.vision.service.VisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "视力检测管理")
@RestController
@RequestMapping("/api/v1/vision")
@RequiredArgsConstructor
public class VisionController {

    private final VisionService visionService;

    @Operation(summary = "视力检测记录列表（支持医院/儿童/姓名/类型/日期筛选）")
    @GetMapping("/records")
    public Result<PageResult<VisionTestRecord>> list(VisionQuery query) {
        return Result.success(visionService.listRecords(query));
    }

    @Operation(summary = "视力检测配对记录（同一检测事件合并左右眼，供医院端列表展示）")
    @GetMapping("/records/grouped")
    public Result<PageResult<VisionRecordGroup>> grouped(VisionQuery query) {
        return Result.success(visionService.listGroupedRecords(query));
    }

    @GetMapping("/records/{id}")
    public Result<VisionTestRecord> get(@PathVariable Long id) {
        return Result.success(visionService.getRecord(id));
    }

    @OperationLog(module = "vision", action = "create", description = "录入视力检测记录")
    @PostMapping("/records")
    public Result<Long> create(@RequestBody VisionTestRecord record) {
        return Result.success(visionService.createRecord(record));
    }

    @GetMapping("/compare")
    public Result<Map<String, Object>> compare(@RequestParam("childId") Long childId, @RequestParam("reserveId") Long reserveId) {
        return Result.success(visionService.compareVision(childId, reserveId));
    }
}
