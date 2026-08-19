package com.careld.store.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.store.dto.CalibrationRequest;
import com.careld.store.dto.DeviceBindRequest;
import com.careld.store.dto.DeviceResponse;
import com.careld.store.entity.TvDevice;

import java.util.List;
import java.util.Map;

/**
 * TV 设备管理服务
 */
public interface DeviceService {

    IPage<DeviceResponse> pageDevices(Long storeId, Integer status, String keyword, Integer page, Integer size);

    List<DeviceResponse> listDevices(Long storeId, Integer status, String keyword);

    DeviceResponse getDeviceById(Long id);

    Long bindDevice(DeviceBindRequest request);

    void updateCalibration(Long id, CalibrationRequest request);

    void recordSync(Long id);

    void unbindDevice(Long id);

    Map<String, Object> deviceSyncSummary(Long storeId);

    TvDevice getEntityById(Long id);

    /**
     * 新增设备（含设备类型、SN、使用寿命等）
     */
    Long createDevice(TvDevice device);

    /**
     * 更新设备信息
     */
    void updateDevice(Long id, TvDevice device);

    /**
     * 删除设备
     */
    void deleteDevice(Long id);

    /**
     * 释放设备（清除医院绑定，设为空闲）
     */
    void releaseDevice(Long id);
}
