package com.careld.sync.service.impl;
import com.careld.sync.dto.SyncUploadRequest;
import com.careld.sync.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {
    @Override
    public Map<String, Object> uploadRecords(SyncUploadRequest request) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (SyncUploadRequest.VisionRecord record : request.getRecords()) {
            Map<String, Object> r = new HashMap<>();
            r.put("localId", record.getLocalId());
            r.put("status", "success");
            r.put("cloudRecordId", "REC" + System.currentTimeMillis());
            results.add(r);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("batchId", request.getBatchId());
        result.put("totalCount", request.getRecords().size());
        result.put("successCount", request.getRecords().size());
        result.put("failCount", 0);
        result.put("results", results);
        return result;
    }
    @Override
    public Map<String, Object> downloadChildren(Long storeId, Long lastSyncTime, Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", List.of());
        result.put("hasMore", false);
        result.put("nextCursor", null);
        result.put("syncTime", System.currentTimeMillis());
        return result;
    }
    @Override
    public Map<String, Object> getSyncConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("autoSyncInterval", 30);
        config.put("maxBatchSize", 100);
        config.put("retryInterval", List.of(60, 300, 900, 3600));
        config.put("maxRetryCount", 5);
        config.put("dataRetentionDays", 90);
        return config;
    }
    @Override
    public void callback(String batchId, Long deviceId, String syncType, String status) {
    }
}
