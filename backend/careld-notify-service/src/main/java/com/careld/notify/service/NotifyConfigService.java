package com.careld.notify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.careld.common.exception.BusinessException;
import com.careld.common.notify.NotifyEventTypes;
import com.careld.notify.dto.NotifyConfigRequest;
import com.careld.notify.entity.StoreNotifyConfig;
import com.careld.notify.mapper.StoreNotifyConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 医院端通知服务配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyConfigService {

    private final StoreNotifyConfigMapper configMapper;

    /** 取配置，缺失时按默认值落库（两开关关闭 + 五个业务通知类型勾选） */
    @Transactional
    public StoreNotifyConfig getOrCreate(Long storeId) {
        StoreNotifyConfig exist = select(storeId);
        if (exist != null) {
            return exist;
        }
        StoreNotifyConfig config = new StoreNotifyConfig();
        config.setStoreId(storeId);
        config.setSmsEnabled(0);
        config.setWechatEnabled(0);
        config.setEnabledTypes(NotifyEventTypes.DEFAULT_ENABLED);
        try {
            configMapper.insert(config);
            return config;
        } catch (Exception e) {
            // 并发初始化撞唯一键：回读已存在行
            StoreNotifyConfig created = select(storeId);
            if (created != null) {
                return created;
            }
            throw e;
        }
    }

    @Transactional
    public StoreNotifyConfig save(Long storeId, NotifyConfigRequest request) {
        if (request.getSmsEnabled() == null && request.getWechatEnabled() == null && request.getEnabledTypes() == null) {
            throw new BusinessException(400, "没有需要保存的内容");
        }
        List<String> types = request.getEnabledTypes();
        if (types != null) {
            List<String> invalid = types.stream().filter(type -> !NotifyEventTypes.isValid(type)).toList();
            if (!invalid.isEmpty()) {
                throw new BusinessException(400, "存在无效的通知类型：" + String.join(",", invalid));
            }
        }
        StoreNotifyConfig config = getOrCreate(storeId);
        if (request.getSmsEnabled() != null) {
            config.setSmsEnabled(toFlag(request.getSmsEnabled()));
        }
        if (request.getWechatEnabled() != null) {
            config.setWechatEnabled(toFlag(request.getWechatEnabled()));
        }
        if (types != null) {
            config.setEnabledTypes(joinTypes(types));
        }
        configMapper.updateById(config);
        return config;
    }

    private StoreNotifyConfig select(Long storeId) {
        return configMapper.selectOne(new LambdaQueryWrapper<StoreNotifyConfig>()
                .eq(StoreNotifyConfig::getStoreId, storeId)
                .orderByAsc(StoreNotifyConfig::getId)
                .last("LIMIT 1"));
    }

    /** 配置中的通知类型列表 */
    public static List<String> parseTypes(String enabledTypes) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(enabledTypes)) {
            return result;
        }
        for (String type : enabledTypes.split(",")) {
            String trimmed = type.trim();
            if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /** 某通知类型是否被勾选（未保存过配置时按默认类型处理） */
    public static boolean isTypeEnabled(String enabledTypes, String eventType) {
        if (eventType == null) {
            return false;
        }
        String source = StringUtils.hasText(enabledTypes) ? enabledTypes : NotifyEventTypes.DEFAULT_ENABLED;
        return parseTypes(source).contains(eventType);
    }

    /** 按枚举顺序去重后存库 */
    private String joinTypes(List<String> types) {
        Set<String> selected = new LinkedHashSet<>(types);
        List<String> ordered = NotifyEventTypes.ALL.stream().filter(selected::contains).toList();
        return String.join(",", ordered);
    }

    private Integer toFlag(Integer value) {
        return value != null && value == 1 ? 1 : 0;
    }
}
