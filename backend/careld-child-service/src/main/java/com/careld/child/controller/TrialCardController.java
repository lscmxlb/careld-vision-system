package com.careld.child.controller;

import com.careld.child.entity.TrialCard;
import com.careld.child.service.TrialCardService;
import com.careld.common.log.OperationLog;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.common.security.DataScopeHelper;
import com.careld.common.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "项目体验卡")
@RestController
@RequestMapping("/api/v1/trial-cards")
@RequiredArgsConstructor
public class TrialCardController {
    private final TrialCardService trialCardService;

    @Value("${encryption.key:careld-vision-encrypt-key-32byte}")
    private String aesKey;

    @Operation(summary = "体验卡列表（后台医院管理，含使用信息）")
    @GetMapping
    @RequirePermission("trialcard:view")
    public Result<PageResult<TrialCard>> list(@RequestParam(value = "status", required = false) Integer status,
                                              @RequestParam(value = "centerId", required = false) Long centerId,
                                              @RequestParam(value = "agentId", required = false) Long agentId,
                                              @RequestParam(value = "storeId", required = false) Long storeId,
                                              @RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "startDate", required = false) String startDate,
                                              @RequestParam(value = "endDate", required = false) String endDate,
                                              @RequestParam(value = "page", defaultValue = "1") Integer page,
                                              @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 数据权限：运营中心/代理商/门店用户各自的组织范围内过滤
        Long effectiveCenterId = DataScopeHelper.resolveCenterId(centerId);
        Long effectiveAgentId = DataScopeHelper.resolveAgentId(agentId);
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        return Result.success(trialCardService.listCards(status, effectiveCenterId, effectiveAgentId, effectiveStoreId,
                keyword == null || keyword.isBlank() ? null : keyword.trim(), startDate, endDate, page, size, aesKey));
    }

    @OperationLog(module = "trialcard", action = "create", description = "发行项目体验卡")
    @Operation(summary = "批量发行体验卡（编号 + 验证码；count 可选 1~10000，同批连续编号）")
    @PostMapping
    @RequirePermission("trialcard:create")
    public Result<List<TrialCard>> issue(@RequestBody Map<String, Object> body) {
        return Result.success(trialCardService.issueCards(longOf(body.get("centerId")), longOf(body.get("agentId")),
                longOf(body.get("storeId")), (String) body.get("areaCode"), (String) body.get("title"),
                (String) body.get("remark"), intOf(body.get("count"))));
    }

    @Operation(summary = "家长端兑换体验卡（档案可用预约次数 +1）")
    @PostMapping("/redeem")
    public Result<Map<String, Object>> redeem(@RequestBody Map<String, Object> body) {
        return Result.success(trialCardService.redeem(longOf(body.get("childId")),
                (String) body.get("cardNo"), (String) body.get("verifyCode")));
    }

    @OperationLog(module = "trialcard", action = "disable", description = "禁用项目体验卡")
    @Operation(summary = "禁用体验卡（仅未兑换，0→3）")
    @PutMapping("/{id}/disable")
    @RequirePermission("trialcard:create")
    public Result<Map<String, Object>> disable(@PathVariable("id") Long id) {
        return Result.success(trialCardService.disableCard(id));
    }

    @OperationLog(module = "trialcard", action = "enable", description = "启用项目体验卡")
    @Operation(summary = "启用体验卡（仅已禁用，3→0）")
    @PutMapping("/{id}/enable")
    @RequirePermission("trialcard:create")
    public Result<Map<String, Object>> enable(@PathVariable("id") Long id) {
        return Result.success(trialCardService.enableCard(id));
    }

    @OperationLog(module = "trialcard", action = "disable-range", description = "批量禁用项目体验卡（编号区间）")
    @Operation(summary = "按编号区间批量禁用体验卡（仅未兑换，返回 total/disabled/skipped）")
    @PostMapping("/disable-range")
    @RequirePermission("trialcard:create")
    public Result<Map<String, Object>> disableRange(@RequestBody Map<String, Object> body) {
        return Result.success(trialCardService.disableRange((String) body.get("startCardNo"),
                (String) body.get("endCardNo")));
    }

    private Long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer intOf(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
