package com.careld.store.service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.store.entity.Store;
import java.util.List;
public interface StoreService {
    Long createStore(Store store);
    void updateStore(Long id, Store store);
    void deleteStore(Long id);
    Store getStoreById(Long id);
    Store getStoreByCode(String storeCode);
    IPage<Store> listStores(Integer status, Long agentId, Long centerId, Long storeId, String keyword, Integer page, Integer size);
    void updateStatus(Long id, Integer status);

    /**
     * 全量门店列表（轻量，供下拉选择），支持按组织链过滤
     */
    List<Store> listAllStores(Long centerId, Long agentId, Long storeId, boolean includeDisabled);

    /**
     * 当前登录用户所属门店
     */
    Store getCurrentStore(Long storeId);
}
