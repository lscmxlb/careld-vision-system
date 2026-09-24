package com.careld.notify.controller;

import com.careld.common.exception.BusinessException;
import com.careld.common.result.Result;
import com.careld.common.security.DataScopeHelper;
import com.careld.notify.dto.PayCreateRequest;
import com.careld.notify.dto.RechargeOrderVO;
import com.careld.notify.service.RechargeOrderService;
import com.careld.notify.service.WechatPayConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 医院端「通知服务 → 续费」：微信扫码支付（Native）下单、查单与支付结果回调
 *
 * <p>回调地址 {@code /api/v1/notify/pay/callback/wechat} 由微信服务器匿名访问
 * （须为 HTTPS 公网入口，由 nginx 转发到本服务），签名验签与 AES-GCM 解密通过后才入账。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notify/pay")
@RequiredArgsConstructor
@Tag(name = "微信支付续费", description = "短信服务余额微信扫码充值")
public class NotifyPayController {

    private final RechargeOrderService orderService;
    private final WechatPayConfigService payConfigService;

    @GetMapping("/status")
    @Operation(summary = "支付通道状态（医院端判断是否可扫码充值）")
    public Result<Map<String, Object>> status(@RequestParam(required = false) Long storeId) {
        WechatPayConfigService.PayRuntime runtime = payConfigService.loadRuntime();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("realReady", runtime.realReady());
        data.put("mockEnabled", runtime.mockEnabled() && !runtime.realReady());
        data.put("notReadyReason", payConfigService.getConfig().getNotReadyReason());
        data.put("unitPrice", com.careld.notify.service.NotifyDispatchService.SMS_UNIT_PRICE);
        return Result.success(data);
    }

    @PostMapping("/orders")
    @Operation(summary = "发起充值下单，返回二维码链接（code_url）")
    public Result<RechargeOrderVO> create(@RequestParam(required = false) Long storeId,
                                          @Valid @RequestBody PayCreateRequest request) {
        return Result.success(orderService.createOrder(resolveStoreId(storeId), request.getAmount()));
    }

    @GetMapping("/orders/{orderNo}")
    @Operation(summary = "查询充值订单（医院端轮询支付结果）")
    public Result<RechargeOrderVO> get(@RequestParam(required = false) Long storeId,
                                       @PathVariable String orderNo) {
        return Result.success(orderService.getOrder(resolveStoreId(storeId), orderNo, false));
    }

    @PostMapping("/orders/{orderNo}/mock-pay")
    @Operation(summary = "模拟支付成功（仅联调期开关打开时可用，用于无证书环境走通链路）")
    public Result<RechargeOrderVO> mockPay(@RequestParam(required = false) Long storeId,
                                           @PathVariable String orderNo) {
        return Result.success(orderService.mockPay(resolveStoreId(storeId), orderNo));
    }

    /** 微信支付结果通知（匿名）：验签通过且订单金额一致才入账 */
    @PostMapping("/callback/wechat")
    @Operation(summary = "微信支付结果回调")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestBody(required = false) String body) {
        String error = orderService.handleWechatCallback(timestamp, nonce, signature, serial, body == null ? "" : body);
        if (error != null && !error.isEmpty()) {
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("code", "FAIL");
            fail.put("message", error);
            // 4xx/5xx 让微信按策略重试，避免漏单
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fail);
        }
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("code", "SUCCESS");
        ok.put("message", "成功");
        return ResponseEntity.ok(ok);
    }

    /** 医院维护类用户强制取本人医院，总部/超管需显式指定医院 */
    private Long resolveStoreId(Long paramStoreId) {
        Long storeId = DataScopeHelper.resolveStoreId(paramStoreId);
        if (storeId == null) {
            throw new BusinessException(400, "未指定医院");
        }
        return storeId;
    }
}
