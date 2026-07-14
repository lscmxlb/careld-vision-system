package com.careld.store.service.impl;
import com.careld.common.exception.BusinessException;
import com.careld.store.entity.Store;
import com.careld.store.mapper.StoreMapper;
import com.careld.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
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
    public List<Store> listStores(Integer status, String keyword) {
        return storeMapper.selectList(null);
    }
    @Override
    public void updateStatus(Long id, Integer status) {
        Store store = new Store(); store.setId(id); store.setStatus(status);
        storeMapper.updateById(store);
    }

    @Override
    public List<Store> listAllStores() {
        return storeMapper.selectList(null);
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
