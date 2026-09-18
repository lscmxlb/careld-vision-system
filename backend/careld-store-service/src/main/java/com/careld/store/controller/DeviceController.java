package com.careld.store.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.common.security.DataScopeHelper;
import com.careld.common.security.UserContext;
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
        // 数据权限：按组织链强制过滤
        // - 门店用户(type=2)：强制 storeId
        // - 代理商(type=5)：强制其 agentId 下所有医院的设备
        // - 运营中心(type=4)：强制其 centerId 下所有医院的设备
        // - 总部/admin：不限制
        Long effectiveStoreId = null;
        Long effectiveAgentId = null;
        Long effectiveCenterId = null;
        if (!DataScopeHelper.isSuperAdmin()) {
            Integer currentType = UserContext.getCurrentUserType();
            if (currentType != null) {
                switch (currentType) {
                    case 2:
                        effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
                        break;
                    case 5:
                        effectiveAgentId = UserContext.getCurrentAgentId();
                        break;
                    case 4:
                        effectiveCenterId = UserContext.getCurrentCenterId();
                        break;
                    default:
                        // 总部用户不限制
                        break;
                }
            }
        } else {
            effectiveStoreId = storeId;
        }
        IPage<DeviceResponse> p = deviceService.pageDevices(effectiveStoreId, effectiveAgentId, effectiveCenterId, status, keyword, page, size);
        return Result.success(PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal()));
    }

    @Operation(summary = "设备详情")
    @GetMapping("/{id}")
    @RequirePermission("device:list:view")
    public Result<DeviceResponse> get(@PathVariable Long id) {
        deviceService.assertDeviceInScope(id);
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
        deviceService.assertDeviceInScope(id);
        deviceService.updateDevice(id, device);
        return Result.success();
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    @RequirePermission("device:list:delete")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.assertDeviceInScope(id);
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
        deviceService.assertDeviceInScope(id);
        deviceService.releaseDevice(id);
        return Result.success();
    }
}
