package com.careld.store.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.store.entity.Store;
import com.careld.store.mapper.StoreMapper;
import com.careld.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {
    private final StoreMapper storeMapper;
    @Override
    public Long createStore(Store store) {
        Store exist = storeMapper.selectByStoreCode(store.getStoreCode());
        if (exist != null) throw new BusinessException(2001, "门店编码已存在");
        storeMapper.insert(store);
        return store.getId();
    }
    @Override
    public void updateStore(Long id, Store store) {
        store.setId(id);
        storeMapper.updateById(store);
    }
    @Override
    public void deleteStore(Long id) { storeMapper.deleteById(id); }
    @Override
    public Store getStoreById(Long id) { return storeMapper.selectById(id); }
    @Override
    public Store getStoreByCode(String storeCode) { return storeMapper.selectByStoreCode(storeCode); }
    @Override
    public IPage<Store> listStores(Integer status, Long agentId, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Store::getStatus, status);
        }
        if (agentId != null) {
            wrapper.eq(Store::getAgentId, agentId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Store::getStoreName, keyword).or().like(Store::getStoreCode, keyword));
        }
        wrapper.orderByDesc(Store::getId);
        IPage<Store> result = storeMapper.selectPage(new Page<>(page, size), wrapper);
        enrichStoresWithOrgNames(result.getRecords());
        return result;
    }
    @Override
    public void updateStatus(Long id, Integer status) {
        Store store = new Store(); store.setId(id); store.setStatus(status);
        storeMapper.updateById(store);
    }

    @Override
    public List<Store> listAllStores() {
        List<Store> stores = storeMapper.selectList(null);
        enrichStoresWithOrgNames(stores);
        return stores;
    }

    private void enrichStoresWithOrgNames(List<Store> stores) {
        if (stores == null || stores.isEmpty()) return;
        // 查询代理商名称
        Map<Long, String> agentNameMap = storeMapper.selectAgentIdAndName().stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("id")).longValue(),
                        r -> (String) r.get("agent_name"),
                        (a, b) -> a));
        // 查询运营中心名称
        Map<Long, String> centerNameMap = storeMapper.selectCenterIdAndName().stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("id")).longValue(),
                        r -> (String) r.get("center_name"),
                        (a, b) -> a));
        // 查询代理商→运营中心映射
        Map<Long, Long> agentToCenterMap = storeMapper.selectAgentCenterMapping().stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("id")).longValue(),
                        r -> ((Number) r.get("center_id")).longValue(),
                        (a, b) -> a));
        for (Store store : stores) {
            store.setAgentName(agentNameMap.getOrDefault(store.getAgentId(), "-"));
            Long centerId = agentToCenterMap.get(store.getAgentId());
            if (centerId != null) {
                store.setCenterName(centerNameMap.getOrDefault(centerId, "-"));
                store.setCenterId(centerId.toString());
            }
        }
    }

    @Override
    public Store getCurrentStore(Long storeId) {
        if (storeId == null) {
            throw new BusinessException(400, "当前用户未绑定门店");
        }
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException(404, "门店不存在");
        }
        return store;
    }
}
