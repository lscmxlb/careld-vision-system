package com.careld.store.controller;
import com.careld.common.result.Result;
import com.careld.store.entity.Store;
import com.careld.store.service.StoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "门店管理")
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService storeService;
    @GetMapping
    public Result<List<Store>> list(@RequestParam(value = "status", required = false) Integer status,
                                    @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(storeService.listStores(status, keyword));
    }
    @GetMapping("/{id}")
    public Result<Store> get(@PathVariable Long id) { return Result.success(storeService.getStoreById(id)); }
    @PostMapping
    public Result<Long> create(@RequestBody Store store) { return Result.success(storeService.createStore(store)); }
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Store store) {
        storeService.updateStore(id, store); return Result.success();
    }
    @PatchMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody Store store) {
        storeService.updateStatus(id, store.getStatus()); return Result.success();
    }
}
