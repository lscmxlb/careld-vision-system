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

    /**
     * 设备分页列表（支持按门店/状态/关键字过滤，含门店名称）
     */
    IPage<DeviceResponse> pageDevices(Long storeId, Integer status, String keyword, Integer page, Integer size);

    /**
     * 全量列表（供下拉/统计，含门店名称）
     */
    List<DeviceResponse> listDevices(Long storeId, Integer status, String keyword);

    /**
     * 设备详情（含门店名称）
     */
    DeviceResponse getDeviceById(Long id);

    /**
     * 设备绑定门店
     */
    Long bindDevice(DeviceBindRequest request);

    /**
     * 更新校准状态/数据
     */
    void updateCalibration(Long id, CalibrationRequest request);

    /**
     * 记录设备同步（更新 lastSyncTime）
     */
    void recordSync(Long id);

    /**
     * 解绑设备（置为禁用）
     */
    void unbindDevice(Long id);

    /**
     * 同步状态摘要（供统计）
     */
    Map<String, Object> deviceSyncSummary(Long storeId);

    /**
     * 原始实体（内部复用）
     */
    TvDevice getEntityById(Long id);
}
