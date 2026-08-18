package com.careld.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.user.entity.Agent;
import com.careld.user.entity.BrandHq;
import com.careld.user.entity.OpsCenter;
import com.careld.user.service.OrgService;
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

    @Operation(summary = "新增总部")
    @PostMapping("/hq")
    public Result<Long> createHq(@RequestBody BrandHq hq) {
        return Result.success(orgService.createHq(hq));
    }

    @Operation(summary = "编辑总部")
    @PutMapping("/hq/{id}")
    public Result<Void> updateHq(@PathVariable Long id, @RequestBody BrandHq hq) {
        orgService.updateHq(id, hq);
        return Result.success();
    }

    @Operation(summary = "删除总部")
    @DeleteMapping("/hq/{id}")
    public Result<Void> deleteHq(@PathVariable Long id) {
        orgService.deleteHq(id);
        return Result.success();
    }

    // ===== 运营中心 =====
    @Operation(summary = "运营中心列表")
    @GetMapping("/centers")
    public Result<PageResult<OpsCenter>> listCenters(
            @RequestParam(value = "hqId", required = false) Long hqId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Page<OpsCenter> result = (Page<OpsCenter>) orgService.listCenters(hqId, keyword, page, size);
        return Result.success(PageResult.of(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @Operation(summary = "全部运营中心（下拉）")
    @GetMapping("/centers/all")
    public Result<List<OpsCenter>> listAllCenters(@RequestParam(value = "hqId", required = false) Long hqId) {
        return Result.success(orgService.listAllCenters(hqId));
    }

    @Operation(summary = "运营中心详情")
    @GetMapping("/centers/{id}")
    public Result<OpsCenter> getCenter(@PathVariable Long id) {
        return Result.success(orgService.getCenterById(id));
    }

    @Operation(summary = "新增运营中心")
    @PostMapping("/centers")
    public Result<Long> createCenter(@RequestBody OpsCenter center) {
        return Result.success(orgService.createCenter(center));
    }

    @Operation(summary = "编辑运营中心")
    @PutMapping("/centers/{id}")
    public Result<Void> updateCenter(@PathVariable Long id, @RequestBody OpsCenter center) {
        orgService.updateCenter(id, center);
        return Result.success();
    }

    @Operation(summary = "删除运营中心")
    @DeleteMapping("/centers/{id}")
    public Result<Void> deleteCenter(@PathVariable Long id) {
        orgService.deleteCenter(id);
        return Result.success();
    }

    // ===== 代理商 =====
    @Operation(summary = "代理商列表")
    @GetMapping("/agents")
    public Result<PageResult<Agent>> listAgents(
            @RequestParam(value = "centerId", required = false) Long centerId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Page<Agent> result = (Page<Agent>) orgService.listAgents(centerId, keyword, page, size);
        return Result.success(PageResult.of(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @Operation(summary = "全部代理商（下拉）")
    @GetMapping("/agents/all")
    public Result<List<Agent>> listAllAgents(@RequestParam(value = "centerId", required = false) Long centerId) {
        return Result.success(orgService.listAllAgents(centerId));
    }

    @Operation(summary = "代理商详情")
    @GetMapping("/agents/{id}")
    public Result<Agent> getAgent(@PathVariable Long id) {
        return Result.success(orgService.getAgentById(id));
    }

    @Operation(summary = "新增代理商")
    @PostMapping("/agents")
    public Result<Long> createAgent(@RequestBody Agent agent) {
        return Result.success(orgService.createAgent(agent));
    }

    @Operation(summary = "编辑代理商")
    @PutMapping("/agents/{id}")
    public Result<Void> updateAgent(@PathVariable Long id, @RequestBody Agent agent) {
        orgService.updateAgent(id, agent);
        return Result.success();
    }

    @Operation(summary = "删除代理商")
    @DeleteMapping("/agents/{id}")
    public Result<Void> deleteAgent(@PathVariable Long id) {
        orgService.deleteAgent(id);
        return Result.success();
    }
}
