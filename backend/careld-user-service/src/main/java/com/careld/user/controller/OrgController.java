package com.careld.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.common.security.DataScopeHelper;
import com.careld.common.security.UserContext;
import com.careld.user.entity.Agent;
import com.careld.user.entity.BrandHq;
import com.careld.user.entity.OpsCenter;
import com.careld.user.service.OrgService;
import com.careld.common.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织架构管理控制器
 */
@Tag(name = "组织架构管理", description = "总部、运营中心、代理商管理")
@RestController
@RequestMapping("/api/v1/org")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    // ===== 总部 =====
    @Operation(summary = "总部列表")
    @GetMapping("/hq")
    public Result<PageResult<BrandHq>> listHq(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Page<BrandHq> result = (Page<BrandHq>) orgService.listHq(keyword, page, size);
        return Result.success(PageResult.of(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @Operation(summary = "全部总部（下拉）")
    @GetMapping("/hq/all")
    public Result<List<BrandHq>> listAllHq() {
        return Result.success(orgService.listAllHq());
    }

    @Operation(summary = "总部详情")
    @GetMapping("/hq/{id}")
    public Result<BrandHq> getHq(@PathVariable Long id) {
        return Result.success(orgService.getHqById(id));
    }

    @OperationLog(module = "org", action = "create", description = "新增总部")
    @Operation(summary = "新增总部")
    @PostMapping("/hq")
    public Result<Long> createHq(@RequestBody BrandHq hq) {
        return Result.success(orgService.createHq(hq));
    }

    @OperationLog(module = "org", action = "update", description = "编辑总部")
    @Operation(summary = "编辑总部")
    @PutMapping("/hq/{id}")
    public Result<Void> updateHq(@PathVariable Long id, @RequestBody BrandHq hq) {
        orgService.updateHq(id, hq);
        return Result.success();
    }

    @OperationLog(module = "org", action = "delete", description = "删除总部")
    @Operation(summary = "删除总部")
    @DeleteMapping("/hq/{id}")
    public Result<Void> deleteHq(@PathVariable Long id) {
        orgService.deleteHq(id);
        return Result.success();
    }

    // ===== 运营中心 =====
    @Operation(summary = "运营中心列表")
    @GetMapping("/centers")
    @RequirePermission("organization:center:view")
    public Result<PageResult<OpsCenter>> listCenters(
            @RequestParam(value = "hqId", required = false) Long hqId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Integer currentType = UserContext.getCurrentUserType();
        // 数据权限：非总部用户只能看自己所属的运营中心
        if (!DataScopeHelper.isSuperAdmin() && currentType != null && currentType != 1) {
            Long ownCenterId = UserContext.getCurrentCenterId();
            try {
                if (ownCenterId != null) {
                    OpsCenter ownCenter = orgService.getCenterById(ownCenterId);
                    Page<OpsCenter> singlePage = new Page<>(page, size, 1);
                    singlePage.setRecords(List.of(ownCenter));
                    return Result.success(PageResult.of(singlePage.getRecords(), singlePage.getCurrent(), singlePage.getSize(), singlePage.getTotal()));
                }
            } catch (Exception e) {
                // 中心不存在时返回空
            }
            return Result.success(PageResult.of(List.of(), 1L, 20L, 0L));
        }
        Page<OpsCenter> result = (Page<OpsCenter>) orgService.listCenters(hqId, keyword, page, size);
        return Result.success(PageResult.of(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @Operation(summary = "全部运营中心（下拉）")
    @GetMapping("/centers/all")
    public Result<List<OpsCenter>> listAllCenters(@RequestParam(value = "hqId", required = false) Long hqId) {
        Integer currentType = UserContext.getCurrentUserType();
        // 数据权限：非总部用户只返回自己所属的运营中心
        if (!DataScopeHelper.isSuperAdmin() && currentType != null && currentType != 1) {
            Long currentCenterId = UserContext.getCurrentCenterId();
            if (currentCenterId != null) {
                try {
                    OpsCenter ownCenter = orgService.getCenterById(currentCenterId);
                    return Result.success(List.of(ownCenter));
                } catch (Exception e) {
                    return Result.success(List.of());
                }
            }
            return Result.success(List.of());
        }
        return Result.success(orgService.listAllCenters(hqId));
    }

    @Operation(summary = "运营中心详情")
    @GetMapping("/centers/{id}")
    @RequirePermission("organization:center:view")
    public Result<OpsCenter> getCenter(@PathVariable Long id) {
        return Result.success(orgService.getCenterById(id));
    }

    @OperationLog(module = "org", action = "create", description = "新增运营中心")
    @Operation(summary = "新增运营中心")
    @PostMapping("/centers")
    @RequirePermission("organization:center:create")
    public Result<Long> createCenter(@RequestBody OpsCenter center) {
        return Result.success(orgService.createCenter(center));
    }

    @OperationLog(module = "org", action = "update", description = "编辑运营中心")
    @Operation(summary = "编辑运营中心")
    @PutMapping("/centers/{id}")
    @RequirePermission("organization:center:update")
    public Result<Void> updateCenter(@PathVariable Long id, @RequestBody OpsCenter center) {
        orgService.updateCenter(id, center);
        return Result.success();
    }

    @OperationLog(module = "org", action = "delete", description = "删除运营中心")
    @Operation(summary = "删除运营中心")
    @DeleteMapping("/centers/{id}")
    @RequirePermission("organization:center:delete")
    public Result<Void> deleteCenter(@PathVariable Long id) {
        orgService.deleteCenter(id);
        return Result.success();
    }

    // ===== 代理商 =====
    @Operation(summary = "代理商列表")
    @GetMapping("/agents")
    @RequirePermission("organization:agent:view")
    public Result<PageResult<Agent>> listAgents(
            @RequestParam(value = "centerId", required = false) Long centerId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 数据权限：代理商/医院维护用户只看自己绑定的代理商
        Integer currentType = UserContext.getCurrentUserType();
        if (!DataScopeHelper.isSuperAdmin() && currentType != null && (currentType == 5 || currentType == 2)
                && UserContext.getCurrentAgentId() != null) {
            try {
                Agent ownAgent = orgService.getAgentById(UserContext.getCurrentAgentId());
                Page<Agent> singlePage = new Page<>(page, size, 1);
                singlePage.setRecords(List.of(ownAgent));
                return Result.success(PageResult.of(singlePage.getRecords(), singlePage.getCurrent(), singlePage.getSize(), singlePage.getTotal()));
            } catch (Exception e) {
                return Result.success(PageResult.of(List.of(), 1L, 20L, 0L));
            }
        }
        // 运营中心用户强制按自己的中心过滤（resolveCenterId 已处理）；代理商/医院用户已在上方早退
        Long effectiveCenterId = DataScopeHelper.resolveCenterId(centerId);
        Page<Agent> result = (Page<Agent>) orgService.listAgents(effectiveCenterId, keyword, page, size);
        return Result.success(PageResult.of(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @Operation(summary = "全部代理商（下拉）")
    @GetMapping("/agents/all")
    public Result<List<Agent>> listAllAgents(@RequestParam(value = "centerId", required = false) Long centerId) {
        Integer currentType = UserContext.getCurrentUserType();
        // 数据权限：代理商/医院维护用户只返回自己绑定的代理商
        if (!DataScopeHelper.isSuperAdmin() && currentType != null && (currentType == 5 || currentType == 2)) {
            Long currentAgentId = UserContext.getCurrentAgentId();
            if (currentAgentId != null) {
                try {
                    Agent ownAgent = orgService.getAgentById(currentAgentId);
                    return Result.success(List.of(ownAgent));
                } catch (Exception e) {
                    return Result.success(List.of());
                }
            }
            return Result.success(List.of());
        }
        // 非总部用户强制按自己的中心过滤
        Long effectiveCenterId = DataScopeHelper.resolveCenterId(centerId);
        return Result.success(orgService.listAllAgents(effectiveCenterId));
    }

    @Operation(summary = "代理商详情")
    @GetMapping("/agents/{id}")
    @RequirePermission("organization:agent:view")
    public Result<Agent> getAgent(@PathVariable Long id) {
        return Result.success(orgService.getAgentById(id));
    }

    @OperationLog(module = "org", action = "create", description = "新增代理商")
    @Operation(summary = "新增代理商")
    @PostMapping("/agents")
    @RequirePermission("organization:agent:create")
    public Result<Long> createAgent(@RequestBody Agent agent) {
        return Result.success(orgService.createAgent(agent));
    }

    @OperationLog(module = "org", action = "update", description = "编辑代理商")
    @Operation(summary = "编辑代理商")
    @PutMapping("/agents/{id}")
    @RequirePermission("organization:agent:update")
    public Result<Void> updateAgent(@PathVariable Long id, @RequestBody Agent agent) {
        orgService.updateAgent(id, agent);
        return Result.success();
    }

    @OperationLog(module = "org", action = "delete", description = "删除代理商")
    @Operation(summary = "删除代理商")
    @DeleteMapping("/agents/{id}")
    @RequirePermission("organization:agent:delete")
    public Result<Void> deleteAgent(@PathVariable Long id) {
        orgService.deleteAgent(id);
        return Result.success();
    }
}
