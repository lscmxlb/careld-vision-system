package com.careld.sync.controller;
import com.careld.common.result.Result;
import com.careld.sync.dto.SyncUploadRequest;
import com.careld.sync.service.SyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@Tag(name = "TV同步管理")
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {
    private final SyncService syncService;
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestBody SyncUploadRequest request) {
        return Result.success(syncService.uploadRecords(request));
    }
    @PostMapping("/download/children")
    public Result<Map<String, Object>> download(@RequestBody Map<String, Object> params) {
        return Result.success(syncService.downloadChildren(
            (Long) params.get("storeId"), (Long) params.get("lastSyncTime"),
            (Integer) params.getOrDefault("page", 1), (Integer) params.getOrDefault("size", 100)));
    }
    @GetMapping("/config")
    public Result<Map<String, Object>> config() { return Result.success(syncService.getSyncConfig()); }
    @PostMapping("/callback")
    public Result<Void> callback(@RequestBody Map<String, Object> params) {
        syncService.callback((String) params.get("batchId"), (Long) params.get("deviceId"),
                           (String) params.get("syncType"), (String) params.get("status"));
        return Result.success();
    }
}
