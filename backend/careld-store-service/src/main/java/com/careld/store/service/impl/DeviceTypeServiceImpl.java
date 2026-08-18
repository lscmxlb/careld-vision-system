package com.careld.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.store.entity.DeviceType;
import com.careld.store.mapper.DeviceTypeMapper;
import com.careld.store.service.DeviceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTypeServiceImpl implements DeviceTypeService {

    private final DeviceTypeMapper deviceTypeMapper;

    @Override
    public IPage<DeviceType> listDeviceTypes(String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<DeviceType> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DeviceType::getTypeName, keyword).or().like(DeviceType::getTypeCode, keyword));
        }
        wrapper.orderByDesc(DeviceType::getId);
        return deviceTypeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<DeviceType> listAllDeviceTypes() {
        return deviceTypeMapper.selectList(new LambdaQueryWrapper<DeviceType>().eq(DeviceType::getStatus, 1));
    }

    @Override
    public DeviceType getDeviceTypeById(Long id) {
        DeviceType deviceType = deviceTypeMapper.selectById(id);
        if (deviceType == null) throw new BusinessException(404, "设备类型不存在");
        return deviceType;
    }

    @Override
    public Long createDeviceType(DeviceType deviceType) {
        deviceTypeMapper.insert(deviceType);
        return deviceType.getId();
    }

    @Override
    public void updateDeviceType(Long id, DeviceType deviceType) {
        deviceType.setId(id);
        deviceTypeMapper.updateById(deviceType);
    }

    @Override
    public void deleteDeviceType(Long id) {
        deviceTypeMapper.deleteById(id);
    }
}
