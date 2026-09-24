package com.careld.notify.controller;

import com.careld.common.log.OperationLog;
import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.notify.dto.PayConfigRequest;
import com.careld.notify.dto.PayConfigVO;
import com.careld.notify.dto.RechargeOrderPageVO;
import com.careld.notify.dto.RechargeOrderQuery;
import com.careld.notify.dto.RechargeOrderVO;
import com.careld.notify.dto.TrialGrantRequest;
import com.careld.notify.channel.WechatPayClient;
import com.careld.notify.service.RechargeOrderService;
import com.careld.notify.service.WechatPayConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台「系统设置」：微信支付配置与充值记录
 */
@Tag(name = "微信支付配置与充值记录", description = "总部维护微信支付凭据，查询各医院扫码充值流水")
@RestController
@RequestMapping("/api/v1/notify/pay/admin")
@RequiredArgsConstructor
public class NotifyPayAdminController {

    private final WechatPayConfigService configService;
    private final RechargeOrderService orderService;
    private final WechatPayClient payClient;

    @Operation(summary = "查询微信支付配置（APIv3 密钥只回掩码）")
    @GetMapping("/config")
    @RequirePermission("settings:view")
    public Result<PayConfigVO> getConfig() {
        return Result.success(configService.getConfig());
    }

    @OperationLog(module = "notify", action = "pay-config", description = "保存微信支付配置")
    @Operation(summary = "保存微信支付配置")
    @PutMapping("/config")
    @RequirePermission("settings:view")
    public Result<PayConfigVO> saveConfig(@RequestBody PayConfigRequest request) {
        configService.saveConfig(request);
        return Result.success(configService.getConfig());
    }

    @Operation(summary = "诊断：校验商户私钥可读性与平台证书下载")
    @GetMapping("/probe")
    @RequirePermission("settings:view")
    public Result<Map<String, Object>> probe() {
        return Result.success(payClient.probe(configService.loadRuntime()));
    }

    @Operation(summary = "分页查询充值记录（各医院扫码充值流水）")
    @GetMapping("/orders")
    @RequirePermission("settings:view")
    public Result<RechargeOrderPageVO> orders(RechargeOrderQuery query) {
        return Result.success(orderService.page(query));
    }

    @OperationLog(module = "notify", action = "trial-grant", description = "短信试用赠送")
    @Operation(summary = "给医院短信账户赠送试用额度（生成备注「试用赠送」的充值记录）")
    @PostMapping("/trial-grant")
    @RequirePermission("settings:view")
    public Result<RechargeOrderVO> trialGrant(@Valid @RequestBody TrialGrantRequest request) {
        return Result.success(orderService.grantTrial(request.getStoreId(), request.getAmount()));
    }
}
