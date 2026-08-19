package com.careld.store.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.common.security.DataScopeHelper;
import com.careld.store.dto.CalibrationRequest;
import com.careld.store.dto.DeviceBindRequest;
import com.careld.store.dto.DeviceResponse;
import com.careld.store.entity.TvDevice;
import com.careld.store.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * TV 设备管理控制器
 */
@Tag(name = "TV设备管理")
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "设备分页列表")
    @GetMapping
    @RequirePermission("device:list:view")
    public Result<PageResult<DeviceResponse>> list(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 数据权限：门店用户注入 storeId
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        IPage<DeviceResponse> p = deviceService.pageDevices(effectiveStoreId, status, keyword, page, size);
        return Result.success(PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal()));
    }

    @Operation(summary = "设备详情")
    @GetMapping("/{id}")
    @RequirePermission("device:list:view")
    public Result<DeviceResponse> get(@PathVariable Long id) {
        return Result.success(deviceService.getDeviceById(id));
    }

    @Operation(summary = "新增设备")
    @PostMapping
    @RequirePermission("device:list:create")
    public Result<Long> create(@RequestBody TvDevice device) {
        return Result.success(deviceService.createDevice(device));
    }

    @Operation(summary = "更新设备")
    @PutMapping("/{id}")
    @RequirePermission("device:list:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody TvDevice device) {
        deviceService.updateDevice(id, device);
        return Result.success();
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    @RequirePermission("device:list:delete")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return Result.success();
    }

    @Operation(summary = "设备绑定门店")
    @PostMapping("/bind")
    public Result<Long> bind(@Valid @RequestBody DeviceBindRequest request) {
        return Result.success(deviceService.bindDevice(request));
    }

    @Operation(summary = "更新设备校准数据")
    @PutMapping("/{id}/calibration")
    public Result<Void> updateCalibration(@PathVariable Long id, @RequestBody CalibrationRequest request) {
        deviceService.updateCalibration(id, request);
        return Result.success();
    }

    @Operation(summary = "触发设备同步")
    @PostMapping("/{id}/sync")
    public Result<Void> sync(@PathVariable Long id) {
        deviceService.recordSync(id);
        return Result.success();
    }

    @Operation(summary = "解绑设备")
    @PostMapping("/{id}/unbind")
    public Result<Void> unbind(@PathVariable Long id) {
        deviceService.unbindDevice(id);
        return Result.success();
    }

    @Operation(summary = "释放设备（设为空闲）")
    @PostMapping("/{id}/release")
    @RequirePermission("device:list:release")
    public Result<Void> release(@PathVariable Long id) {
        deviceService.releaseDevice(id);
        return Result.success();
    }
}
