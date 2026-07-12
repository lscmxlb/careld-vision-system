package com.careld.store;

import com.careld.store.entity.Store;
import com.careld.store.service.StoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class StoreServiceTest {

    @Autowired
    private StoreService storeService;

    @Test
    void testStoreServiceExists() {
        assertNotNull(storeService);
    }

    @Test
    void testCreateStore() {
        Store store = new Store();
        store.setStoreCode("TEST" + System.currentTimeMillis());
        store.setStoreName("测试门店");
        store.setProvinceName("北京市");
        store.setCityName("北京市");
        store.setAddress("测试地址");
        store.setContactName("测试联系人");
        store.setContactPhone("13800138000");
        store.setStatus(1);
        
        assertDoesNotThrow(() -> {
            // 实际测试需要连接数据库
        });
    }
}
