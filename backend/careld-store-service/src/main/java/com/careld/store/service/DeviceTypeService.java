package com.careld.store.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.store.entity.DeviceType;

import java.util.List;

public interface DeviceTypeService {
    IPage<DeviceType> listDeviceTypes(String keyword, Integer page, Integer size);
    List<DeviceType> listAllDeviceTypes();
    DeviceType getDeviceTypeById(Long id);
    Long createDeviceType(DeviceType deviceType);
    void updateDeviceType(Long id, DeviceType deviceType);
    void deleteDeviceType(Long id);
}
