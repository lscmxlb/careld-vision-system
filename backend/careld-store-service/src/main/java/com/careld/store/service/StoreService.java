package com.careld.store.service;
import com.careld.store.entity.Store;
import java.util.List;
public interface StoreService {
    Long createStore(Store store);
    void updateStore(Long id, Store store);
    void deleteStore(Long id);
    Store getStoreById(Long id);
    Store getStoreByCode(String storeCode);
    List<Store> listStores(Integer status, String keyword);
    void updateStatus(Long id, Integer status);
}
