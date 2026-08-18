package com.careld.store.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.store.entity.DeviceType;
import com.careld.store.service.DeviceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备类型管理控制器
 */
@Tag(name = "设备类型管理")
@RestController
@RequestMapping("/api/v1/device-types")
@RequiredArgsConstructor
public class DeviceTypeController {

    private final DeviceTypeService deviceTypeService;

    @Operation(summary = "设备类型列表")
    @GetMapping
    public Result<PageResult<DeviceType>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Page<DeviceType> result = (Page<DeviceType>) deviceTypeService.listDeviceTypes(keyword, page, size);
        return Result.success(PageResult.of(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @Operation(summary = "全部设备类型（下拉）")
    @GetMapping("/all")
    public Result<List<DeviceType>> listAll() {
        return Result.success(deviceTypeService.listAllDeviceTypes());
    }

    @Operation(summary = "设备类型详情")
    @GetMapping("/{id}")
    public Result<DeviceType> get(@PathVariable Long id) {
        return Result.success(deviceTypeService.getDeviceTypeById(id));
    }

    @Operation(summary = "新增设备类型")
    @PostMapping
    public Result<Long> create(@RequestBody DeviceType deviceType) {
        return Result.success(deviceTypeService.createDeviceType(deviceType));
    }

    @Operation(summary = "编辑设备类型")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DeviceType deviceType) {
        deviceTypeService.updateDeviceType(id, deviceType);
        return Result.success();
    }

    @Operation(summary = "删除设备类型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceTypeService.deleteDeviceType(id);
        return Result.success();
    }
}
