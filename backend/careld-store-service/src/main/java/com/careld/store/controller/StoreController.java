package com.careld.store.controller;

import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.common.security.DataScopeHelper;
import com.careld.common.security.UserContext;
import com.careld.store.entity.Store;
import com.careld.store.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门店管理控制器
 */
@Tag(name = "门店管理")
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "门店列表")
    @GetMapping
    @RequirePermission("store:list:view")
    public Result<PageResult<Store>> list(@RequestParam(value = "status", required = false) Integer status,
                                          @RequestParam(value = "agentId", required = false) Long agentId,
                                          @RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(value = "page", defaultValue = "1") Integer page,
                                          @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 数据权限：沿组织绑定链严格向下过滤
        Integer currentType = UserContext.getCurrentUserType();
        Long scopeCenterId = null;
        Long scopeStoreId = null;
        if (!DataScopeHelper.isSuperAdmin() && currentType != null && currentType != 1) {
            switch (currentType) {
                case 4:
                    // 运营中心用户：仅本中心下的门店
                    scopeCenterId = UserContext.getCurrentCenterId();
                    break;
                case 5:
                    // 代理商用户：仅本代理商下的门店（resolveAgentId 强制）
                    break;
                case 2:
                    // 医院维护用户：仅自己这家医院
                    scopeStoreId = UserContext.getCurrentStoreId();
                    break;
                default:
                    break;
            }
        }
        // 代理商用户强制按自己的 agentId 过滤；运营中心用户可按前端传入的 agentId 筛选（centerId 同时限制范围）
        Long effectiveAgentId = DataScopeHelper.resolveAgentId(agentId);
        var p = storeService.listStores(status, effectiveAgentId, scopeCenterId, scopeStoreId, keyword, page, size);
        return Result.success(PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal()));
    }

    @Operation(summary = "全量门店列表（下拉用）")
    @GetMapping("/all")
    public Result<List<Store>> all() {
        // 数据权限：沿组织绑定链严格向下过滤
        Integer currentType = UserContext.getCurrentUserType();
        Long scopeCenterId = null;
        Long scopeAgentId = null;
        Long scopeStoreId = null;
        if (!DataScopeHelper.isSuperAdmin() && currentType != null && currentType != 1) {
            switch (currentType) {
                case 4:
                    scopeCenterId = UserContext.getCurrentCenterId();
                    break;
                case 5:
                    scopeAgentId = UserContext.getCurrentAgentId();
                    break;
                case 2:
                    scopeStoreId = UserContext.getCurrentStoreId();
                    break;
                default:
                    break;
            }
        }
        return Result.success(storeService.listAllStores(scopeCenterId, scopeAgentId, scopeStoreId));
    }

    @Operation(summary = "当前登录用户所属门店")
    @GetMapping("/current")
    public Result<Store> current() {
        return Result.success(storeService.getCurrentStore(UserContext.getCurrentStoreId()));
    }

    @GetMapping("/{id}")
    @RequirePermission("store:list:view")
    public Result<Store> get(@PathVariable Long id) {
        return Result.success(storeService.getStoreById(id));
    }

    @PostMapping
    @RequirePermission("store:list:create")
    public Result<Long> create(@RequestBody Store store) {
        return Result.success(storeService.createStore(store));
    }

    @PutMapping("/{id}")
    @RequirePermission("store:list:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody Store store) {
        storeService.updateStore(id, store);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @RequirePermission("store:list:update")
    public Result<Void> status(@PathVariable Long id, @RequestBody Store store) {
        storeService.updateStatus(id, store.getStatus());
        return Result.success();
    }
}

