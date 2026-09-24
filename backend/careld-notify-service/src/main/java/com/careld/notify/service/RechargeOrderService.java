package com.careld.notify.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.careld.common.exception.BusinessException;
import com.careld.common.result.PageResult;
import com.careld.notify.channel.WechatPayClient;
import com.careld.notify.dto.RechargeOrderPageVO;
import com.careld.notify.dto.RechargeOrderQuery;
import com.careld.notify.dto.RechargeOrderVO;
import com.careld.notify.entity.StoreRechargeOrder;
import com.careld.notify.mapper.NotifySourceMapper;
import com.careld.notify.mapper.StoreRechargeOrderMapper;
import com.careld.notify.service.WechatPayConfigService.PayRuntime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 医院短信服务充值订单（微信扫码支付）
 *
 * <p>下单 → 展示二维码 → 轮询订单状态；微信回调（或回调丢失时主动查单）确认支付成功后
 * 才把金额计入医院账户余额。入账靠「订单状态 0→1 只能成功一次」的条件更新保证幂等，
 * 重复回调/并发行不会重复充值。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeOrderService {

    /** 单笔充值上限（元），与请求校验保持一致 */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000");
    /** 订单有效期（分钟） */
    private static final int EXPIRE_MINUTES = 120;
    /** 查单同步最小间隔（毫秒），避免前端轮询把微信查单接口打爆 */
    private static final long QUERY_INTERVAL_MS = 3000;
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** 微信支付侧的交易状态：这些状态下订单不会再支付成功 */
    private static final List<String> CLOSED_STATES = List.of("CLOSED", "REVOKED", "PAYERROR");

    private final StoreRechargeOrderMapper orderMapper;
    private final NotifySourceMapper sourceMapper;
    private final NotifyAccountService accountService;
    private final WechatPayConfigService payConfigService;
    private final WechatPayClient payClient;
    /** 入账的「置已支付 + 累加余额」必须在同一事务里，且不能圈住查单等 HTTP 调用 */
    private final TransactionTemplate transactionTemplate;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Long> lastQueryAt = new ConcurrentHashMap<>();

    /** 医院端下单：真实支付就绪走微信 Native，否则仅在开启模拟支付时生成模拟订单 */
    @Transactional
    public RechargeOrderVO createOrder(Long storeId, BigDecimal amount) {
        BigDecimal normalized = normalizeAmount(amount);
        PayRuntime runtime = payConfigService.loadRuntime();
        boolean realReady = runtime.realReady();
        if (!realReady && !runtime.mockEnabled()) {
            throw new BusinessException(1005, notReadyMessage(runtime));
        }

        StoreRechargeOrder order = new StoreRechargeOrder();
        order.setStoreId(storeId);
        order.setOrderNo(generateOrderNo());
        order.setAmount(normalized);
        order.setStatus(0);
        order.setExpireAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        order.setMockFlag(realReady ? 0 : 1);

        if (realReady) {
            String storeName = sourceMapper.selectStoreName(storeId);
            String description = (StringUtils.hasText(storeName) ? storeName + "-" : "") + "短信服务续费";
            if (description.length() > 60) {
                description = description.substring(0, 60);
            }
            WechatPayClient.NativeOrderResult result = payClient.createNativeOrder(
                    runtime, order.getOrderNo(), description, toFen(normalized));
            if (!result.ok()) {
                log.error("[PAY] Native 下单失败 storeId={} orderNo={} error={}", storeId, order.getOrderNo(), result.error());
                throw new BusinessException(1005, "微信支付下单失败：" + result.error());
            }
            order.setCodeUrl(result.codeUrl());
            order.setPrepayId(result.prepayId());
        } else {
            // 模拟支付仅在「未启用真实支付 + 联调开关打开」时可用，用于无证书环境走通全链路
            log.warn("[PAY][MOCK] 生成模拟支付订单 storeId={} orderNo={} amount={}", storeId, order.getOrderNo(), normalized);
            order.setCodeUrl("mockpay://" + order.getOrderNo());
        }

        orderMapper.insert(order);
        return RechargeOrderVO.of(order);
    }

    /** 查询订单（医院端轮询）：待支付且为真实支付时按节流同步微信侧状态 */
    public RechargeOrderVO getOrder(Long storeId, String orderNo, boolean allowAnyStore) {
        StoreRechargeOrder order = requireOrder(orderNo);
        if (!allowAnyStore && !order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "无权查看该充值订单");
        }
        if (order.getStatus() == 0 && order.getMockFlag() != null && order.getMockFlag() == 0) {
            syncFromWechat(order);
            order = requireOrder(orderNo);
        }
        return RechargeOrderVO.of(order);
    }

    /** 模拟支付成功（仅联调期：开关打开且真实支付未就绪时可用） */
    @Transactional
    public RechargeOrderVO mockPay(Long storeId, String orderNo) {
        StoreRechargeOrder order = requireOrder(orderNo);
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "无权操作该充值订单");
        }
        PayRuntime runtime = payConfigService.loadRuntime();
        if (!runtime.mockEnabled() || runtime.realReady()) {
            throw new BusinessException(1005, "模拟支付未开启（正式支付环境不允许模拟到账）");
        }
        if (order.getStatus() != 0) {
            return RechargeOrderVO.of(order);
        }
        confirmPaid(order, null, "MOCK_SUCCESS", null, LocalDateTime.now(), true);
        return RechargeOrderVO.of(requireOrder(orderNo));
    }

    /**
     * 微信支付结果回调处理
     *
     * @return 入参若有效则返回空字符串，否则返回拒绝原因（回 4xx/5xx 让微信重试）
     */
    @Transactional
    public String handleWechatCallback(String timestamp, String nonce, String signature, String serial, String body) {
        PayRuntime runtime = payConfigService.loadRuntime();
        if (!runtime.realReady()) {
            return "微信支付未配置，拒绝回调";
        }
        String verifyError = payClient.verifyCallback(runtime, timestamp, nonce, signature, serial, body);
        if (StringUtils.hasText(verifyError)) {
            log.warn("[PAY] 回调验签失败：{}", verifyError);
            return verifyError;
        }
        JSONObject payload = JSON.parseObject(body);
        if (!"TRANSACTION.SUCCESS".equals(payload.getString("event_type"))) {
            // 非成功事件（如退款）当前业务不处理，回成功避免微信重复推送
            return "";
        }
        JSONObject resource = payload.getJSONObject("resource");
        if (resource == null) {
            return "回调缺少 resource";
        }
        String plain;
        try {
            plain = payClient.decryptResource(runtime, resource.getString("ciphertext"),
                    resource.getString("nonce"), resource.getString("associated_data"));
        } catch (Exception e) {
            log.error("[PAY] 回调 resource 解密失败", e);
            return "回调 resource 解密失败";
        }
        JSONObject trade = JSON.parseObject(plain);
        String orderNo = trade.getString("out_trade_no");
        String tradeState = trade.getString("trade_state");
        if (!"SUCCESS".equals(tradeState)) {
            return "";
        }
        StoreRechargeOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("[PAY] 回调订单不存在 orderNo={}", orderNo);
            return "订单不存在：" + orderNo;
        }
        Long paidFen = trade.getJSONObject("amount") == null ? null : trade.getJSONObject("amount").getLong("total");
        Long expectFen = order.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (paidFen != null && !paidFen.equals(expectFen)) {
            log.error("[PAY] 回调金额不符 orderNo={} 期望={}分 实际={}分", orderNo, expectFen, paidFen);
            return "支付金额与订单不一致";
        }
        String payerOpenid = trade.getJSONObject("payer") == null ? null : trade.getJSONObject("payer").getString("openid");
        LocalDateTime paidAt = parseSuccessTime(trade.getString("success_time"));
        confirmPaid(order, trade.getString("transaction_id"), tradeState, payerOpenid, paidAt, false);
        return "";
    }

    /** 充值记录分页（管理后台） */
    public RechargeOrderPageVO page(RechargeOrderQuery query) {
        Map<String, Object> summary = orderMapper.summaryByQuery(query);
        long total = summary == null || summary.get("total") == null ? 0 : ((Number) summary.get("total")).longValue();
        List<StoreRechargeOrder> rows = total == 0 ? List.of() : orderMapper.selectPageByQuery(query);

        RechargeOrderPageVO vo = new RechargeOrderPageVO();
        vo.setList(rows.stream().map(RechargeOrderVO::of).toList());
        int safeSize = query.getSafeSize();
        vo.setPagination(new PageResult.Pagination(query.getOffset() / safeSize + 1, safeSize, total));
        Object paid = summary == null ? null : summary.get("paidAmount");
        vo.setPaidAmount(paid == null ? BigDecimal.ZERO : new BigDecimal(paid.toString()).setScale(2, RoundingMode.HALF_UP));
        return vo;
    }

    // ==================== 内部实现 ====================

    /** 主动查单同步：支付成功则入账，微信侧已关单则关闭订单 */
    private void syncFromWechat(StoreRechargeOrder order) {
        PayRuntime runtime = payConfigService.loadRuntime();
        if (!runtime.realReady()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastQueryAt.get(order.getOrderNo());
        if (last != null && now - last < QUERY_INTERVAL_MS) {
            return;
        }
        lastQueryAt.put(order.getOrderNo(), now);
        WechatPayClient.QueryOrderResult result = payClient.queryOrder(runtime, order.getOrderNo());
        if (!result.ok()) {
            log.warn("[PAY] 查单失败 orderNo={}: {}", order.getOrderNo(), result.error());
            return;
        }
        if ("SUCCESS".equals(result.tradeState())) {
            confirmPaid(order, result.transactionId(), result.tradeState(), result.payerOpenid(), result.paidAt(), false);
        } else if (CLOSED_STATES.contains(result.tradeState())) {
            orderMapper.markClosed(order.getId(), result.tradeState());
        }
    }

    /** 支付成功入账：状态 0→1 条件更新成功者才计费，保证幂等；两步写入同一事务 */
    private void confirmPaid(StoreRechargeOrder order, String transactionId, String tradeState,
                             String payerOpenid, LocalDateTime paidAt, boolean mock) {
        Boolean credited = transactionTemplate.execute(status -> {
            int updated = orderMapper.markPaid(order.getId(), transactionId, tradeState, payerOpenid, paidAt, mock ? 1 : 0);
            if (updated == 0) {
                return false;
            }
            accountService.recharge(order.getStoreId(), order.getAmount());
            return true;
        });
        if (Boolean.TRUE.equals(credited)) {
            log.info("[PAY] 充值到账 storeId={} orderNo={} amount={} txn={} mock={}",
                    order.getStoreId(), order.getOrderNo(), order.getAmount(), transactionId, mock);
        } else {
            log.info("[PAY] 订单已处理过，跳过重复入账 orderNo={}", order.getOrderNo());
        }
    }

    private StoreRechargeOrder requireOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(400, "缺少订单号");
        }
        StoreRechargeOrder order = orderMapper.selectByOrderNo(orderNo.trim());
        if (order == null) {
            throw new BusinessException(404, "充值订单不存在");
        }
        return order;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(400, "请输入正确的充值金额");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.DOWN);
        if (normalized.compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessException(400, "单次充值金额不能低于 1 元");
        }
        if (normalized.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException(400, "单次充值金额不能超过 10000 元");
        }
        return normalized;
    }

    /** 元 → 分（微信支付金额单位为分） */
    private int toFen(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).intValueExact();
    }

    private String generateOrderNo() {
        String suffix = String.format("%06d", secureRandom.nextInt(1_000_000));
        return "RC" + LocalDateTime.now().format(ORDER_NO_TIME) + suffix;
    }

    private LocalDateTime parseSuccessTime(String successTime) {
        if (!StringUtils.hasText(successTime)) {
            return LocalDateTime.now();
        }
        try {
            return java.time.OffsetDateTime.parse(successTime,
                    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String notReadyMessage(PayRuntime runtime) {
        return "总部尚未开通微信支付：" + payConfigService.getConfig().getNotReadyReason()
                + "，请联系总部管理员完成配置后再充值";
    }
}
