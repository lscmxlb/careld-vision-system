package com.careld.sync.service;
import com.careld.sync.dto.SyncUploadRequest;
import java.util.Map;
public interface SyncService {
    Map<String, Object> uploadRecords(SyncUploadRequest request);
    Map<String, Object> downloadChildren(Long storeId, Long lastSyncTime, Integer page, Integer size);
    Map<String, Object> getSyncConfig();
    void callback(String batchId, Long deviceId, String syncType, String status);
}
