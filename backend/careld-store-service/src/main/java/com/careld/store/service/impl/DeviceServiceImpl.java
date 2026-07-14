package com.careld.store.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.store.dto.CalibrationRequest;
import com.careld.store.dto.DeviceBindRequest;
import com.careld.store.dto.DeviceResponse;
import com.careld.store.entity.Store;
import com.careld.store.entity.TvDevice;
import com.careld.store.mapper.StoreMapper;
import com.careld.store.mapper.TvDeviceMapper;
import com.careld.store.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TV 设备管理服务实现
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final TvDeviceMapper tvDeviceMapper;
    private final StoreMapper storeMapper;

    @Override
    public IPage<DeviceResponse> pageDevices(Long storeId, Integer status, String keyword, Integer page, Integer size) {
        Page<TvDevice> p = new Page<>(page == null ? 1 : page, size == null ? 20 : size);
        tvDeviceMapper.selectPage(p, buildWrapper(storeId, status, keyword));
        List<TvDevice> records = p.getRecords();
        Map<Long, String> nameMap = resolveStoreNames(records);
        List<DeviceResponse> list = records.stream().map(d -> toResponse(d, nameMap)).collect(Collectors.toList());

        Page<DeviceResponse> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(list);
        return result;
    }

    @Override
    public List<DeviceResponse> listDevices(Long storeId, Integer status, String keyword) {
        List<TvDevice> devices = tvDeviceMapper.selectList(buildWrapper(storeId, status, keyword));
        Map<Long, String> nameMap = resolveStoreNames(devices);
        return devices.stream().map(d -> toResponse(d, nameMap)).collect(Collectors.toList());
    }

    @Override
    public DeviceResponse getDeviceById(Long id) {
        TvDevice device = getEntityById(id);
        Map<Long, String> nameMap = resolveStoreNames(Collections.singletonList(device));
        return toResponse(device, nameMap);
    }

    @Override
    public Long bindDevice(DeviceBindRequest request) {
        TvDevice device = tvDeviceMapper.selectByDeviceCode(request.getDeviceCode());
        if (device == null) {
            device = new TvDevice();
            device.setDeviceCode(request.getDeviceCode());
            device.setStoreId(request.getStoreId());
            device.setDeviceName(StringUtils.hasText(request.getDeviceName()) ? request.getDeviceName() : request.getDeviceCode());
            device.setCalibrationStatus(0);
            device.setStatus(1);
            device.setBindTime(LocalDateTime.now());
            device.setLastOnlineTime(LocalDateTime.now());
            tvDeviceMapper.insert(device);
        } else {
            device.setStoreId(request.getStoreId());
            if (StringUtils.hasText(request.getDeviceName())) {
                device.setDeviceName(request.getDeviceName());
            }
            device.setStatus(1);
            device.setBindTime(LocalDateTime.now());
            device.setLastOnlineTime(LocalDateTime.now());
            tvDeviceMapper.updateById(device);
        }
        return device.getId();
    }

    @Override
    public void updateCalibration(Long id, CalibrationRequest request) {
        TvDevice device = getEntityById(id);
        if (request.getCalibrationStatus() != null) {
            device.setCalibrationStatus(request.getCalibrationStatus());
        }
        if (request.getCalibrationData() != null) {
            device.setCalibrationData(JSON.toJSONString(request.getCalibrationData()));
        }
        tvDeviceMapper.updateById(device);
    }

    @Override
    public void recordSync(Long id) {
        TvDevice device = getEntityById(id);
        device.setLastSyncTime(LocalDateTime.now());
        device.setLastOnlineTime(LocalDateTime.now());
        tvDeviceMapper.updateById(device);
    }

    @Override
    public void unbindDevice(Long id) {
        TvDevice device = getEntityById(id);
        device.setStatus(0);
        tvDeviceMapper.updateById(device);
    }

    @Override
    public Map<String, Object> deviceSyncSummary(Long storeId) {
        List<TvDevice> devices = tvDeviceMapper.selectList(buildWrapper(storeId, null, null));
        long total = devices.size();
        long online = devices.stream().filter(d -> d.getLastOnlineTime() != null
                && d.getLastOnlineTime().isAfter(LocalDateTime.now().minusHours(2))).count();
        long calibrated = devices.stream().filter(d -> d.getCalibrationStatus() != null
                && d.getCalibrationStatus() == 1).count();
        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("online", online);
        summary.put("calibrated", calibrated);
        return summary;
    }

    @Override
    public TvDevice getEntityById(Long id) {
        TvDevice device = tvDeviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(404, "设备不存在");
        }
        return device;
    }

    private LambdaQueryWrapper<TvDevice> buildWrapper(Long storeId, Integer status, String keyword) {
        LambdaQueryWrapper<TvDevice> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(TvDevice::getStoreId, storeId);
        }
        if (status != null) {
            wrapper.eq(TvDevice::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TvDevice::getDeviceCode, keyword)
                    .or().like(TvDevice::getDeviceName, keyword));
        }
        wrapper.orderByDesc(TvDevice::getId);
        return wrapper;
    }

    private Map<Long, String> resolveStoreNames(List<TvDevice> devices) {
        if (devices.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> storeIds = devices.stream()
                .map(TvDevice::getStoreId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Store> stores = storeMapper.selectBatchIds(storeIds);
        return stores.stream().collect(Collectors.toMap(Store::getId, Store::getStoreName, (a, b) -> a));
    }

    private DeviceResponse toResponse(TvDevice d, Map<Long, String> nameMap) {
        DeviceResponse r = new DeviceResponse();
        r.setId(d.getId());
        r.setDeviceCode(d.getDeviceCode());
        r.setDeviceName(d.getDeviceName());
        r.setStoreId(d.getStoreId());
        r.setStoreName(nameMap.get(d.getStoreId()));
        r.setAndroidVersion(d.getAndroidVersion());
        r.setScreenResolution(d.getScreenResolution());
        r.setScreenSize(d.getScreenSize());
        r.setAppVersion(d.getAppVersion());
        r.setCalibrationStatus(d.getCalibrationStatus());
        if (StringUtils.hasText(d.getCalibrationData())) {
            try {
                r.setCalibrationData(JSON.parse(d.getCalibrationData()));
            } catch (Exception e) {
                r.setCalibrationData(d.getCalibrationData());
            }
        }
        r.setLastOnlineTime(d.getLastOnlineTime());
        r.setLastSyncTime(d.getLastSyncTime());
        r.setStatus(d.getStatus());
        r.setBindTime(d.getBindTime());
        r.setCreatedAt(d.getCreatedAt());
        return r;
    }
}
