package com.careld.notify.service;

import com.careld.common.entity.NotifyTask;
import com.careld.common.notify.NotifyEventTypes;
import com.careld.common.utils.MaskUtil;
import com.careld.notify.channel.SendOutcome;
import com.careld.notify.channel.SmsChannelSender;
import com.careld.notify.channel.WechatChannelSender;
import com.careld.notify.entity.NotifyRecord;
import com.careld.notify.entity.StoreNotifyConfig;
import com.careld.notify.mapper.NotifySourceMapper;
import com.careld.notify.mapper.NotifyTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知任务调度：轮询 notify_task 待发送队列，按医院配置逐条发送短信/微信并计费
 *
 * <p>设计要点：
 * <ul>
 *   <li>任务由业务服务在其本地事务内落库（Outbox），本服务只负责消费，失败不阻塞业务；</li>
 *   <li>抢占式领取（claim）保证多实例/重复调度下同一任务只被处理一次；</li>
 *   <li>短信先按 0.10 元/条预扣费，余额不足则停发短信并记失败；发送失败退回预扣费用；</li>
 *   <li>微信模板消息免费，家长未绑定公众号时记失败，不影响短信；</li>
 *   <li>通道未配置真实凭据时模拟发送成功并计费，备注「模拟通道」。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyDispatchService {

    /** 短信单价：0.10 元/条 */
    public static final BigDecimal SMS_UNIT_PRICE = new BigDecimal("0.10");

    private static final int CHANNEL_SMS = 1;
    private static final int CHANNEL_WECHAT = 2;
    private static final int TASK_DONE = 1;
    private static final int TASK_FAILED = 2;
    private static final int TASK_SKIPPED = 3;
    private static final int STALE_MINUTES = 5;

    private final NotifyTaskMapper taskMapper;
    private final NotifySourceMapper sourceMapper;
    private final NotifyConfigService configService;
    private final NotifyAccountService accountService;
    private final NotifyRecordService recordService;
    private final NotifyChannelRuntimeService runtimeService;
    private final NotifyMessageRenderer renderer;
    private final NotifyCipherHelper cipherHelper;
    private final SmsChannelSender smsSender;
    private final WechatChannelSender wechatSender;

    @Value("${careld.notify.batch-size:20}")
    private int batchSize;

    @Value("${careld.notify.max-retry:3}")
    private int maxRetry;

    @Scheduled(fixedDelayString = "${careld.notify.dispatch-interval-ms:10000}",
            initialDelayString = "${careld.notify.dispatch-initial-delay-ms:5000}")
    public void dispatch() {
        try {
            taskMapper.requeueStale(STALE_MINUTES);
            List<NotifyTask> pending = taskMapper.selectPending(batchSize);
            if (pending.isEmpty()) {
                return;
            }
            // 短信模板按事件取（各事件文案不同），同一批次内相同事件复用一次配置读取
            Map<String, NotifyChannelRuntimeService.SmsRuntime> smsRuntimes = new HashMap<>();
            NotifyChannelRuntimeService.WechatRuntime wechatRuntime = runtimeService.loadWechatRuntime();
            for (NotifyTask task : pending) {
                if (taskMapper.claim(task.getId()) == 0) {
                    continue;
                }
                try {
                    process(task, smsRuntimes, wechatRuntime);
                } catch (Exception e) {
                    log.error("通知任务处理异常 taskId={} event={}", task.getId(), task.getEventType(), e);
                    handleUnexpected(task, e);
                }
            }
        } catch (Exception e) {
            log.warn("通知任务调度异常: {}", e.getMessage());
        }
    }

    /** 处理单个任务：按开关与通知类型判定通道，逐通道发送并落发送记录 */
    private void process(NotifyTask task, Map<String, NotifyChannelRuntimeService.SmsRuntime> smsRuntimes,
                         NotifyChannelRuntimeService.WechatRuntime wechatRuntime) {
        StoreNotifyConfig config = configService.getOrCreate(task.getStoreId());
        boolean typeEnabled = NotifyConfigService.isTypeEnabled(config.getEnabledTypes(), task.getEventType());
        boolean smsOn = typeEnabled && isOn(config.getSmsEnabled());
        boolean wechatOn = typeEnabled && isOn(config.getWechatEnabled());
        if (!smsOn && !wechatOn) {
            taskMapper.finish(task.getId(), TASK_SKIPPED, retryCount(task),
                    typeEnabled ? "两个通知通道均未开启" : "该通知类型未启用");
            return;
        }

        NotifySourceMapper.ChildContact child = sourceMapper.selectChildContact(task.getChildId());
        if (child == null) {
            taskMapper.finish(task.getId(), TASK_SKIPPED, retryCount(task), "儿童档案不存在或已隐藏");
            return;
        }
        Long storeId = child.getStoreId() != null ? child.getStoreId() : task.getStoreId();
        String storeNameDb = sourceMapper.selectStoreName(storeId);
        String storeName = StringUtils.hasText(storeNameDb) ? storeNameDb : "本医院";
        // 短信变量用真实姓名（模板示例均为全名），解密失败回退脱敏值
        NotifyMessageRenderer.RenderedMessage message = renderer.render(task, child.getNameMask(), storeName,
                cipherHelper.decrypt(child.getNameEncrypted()));

        List<String> outcomes = new ArrayList<>();
        int successCount = 0;

        if (smsOn) {
            String phone = resolveParentPhone(child);
            if (!StringUtils.hasText(phone)) {
                saveRecord(task, storeId, child, CHANNEL_SMS, child.getPhoneMask(), message,
                        false, BigDecimal.ZERO, null, "无可用家长手机号");
                outcomes.add("短信：无可用家长手机号");
            } else if (!accountService.tryDeduct(storeId, SMS_UNIT_PRICE)) {
                saveRecord(task, storeId, child, CHANNEL_SMS, MaskUtil.maskPhone(phone), message,
                        false, BigDecimal.ZERO, null, "短信余额不足，已停发短信");
                outcomes.add("短信：余额不足未发送");
            } else {
                NotifyChannelRuntimeService.SmsRuntime smsRuntime = smsRuntimes.computeIfAbsent(
                        String.valueOf(task.getEventType()), runtimeService::loadSmsRuntimeFor);
                SendOutcome outcome = smsSender.send(phone, task.getEventType(), message.vars(),
                        message.content(), smsRuntime);
                if (outcome.success()) {
                    successCount++;
                    saveRecord(task, storeId, child, CHANNEL_SMS, MaskUtil.maskPhone(phone), message,
                            true, SMS_UNIT_PRICE, outcome.remark(), null);
                    outcomes.add("短信：发送成功");
                } else {
                    accountService.refund(storeId, SMS_UNIT_PRICE);
                    saveRecord(task, storeId, child, CHANNEL_SMS, MaskUtil.maskPhone(phone), message,
                            false, BigDecimal.ZERO, null, outcome.failReason());
                    outcomes.add("短信：发送失败");
                }
            }
        }

        if (wechatOn) {
            String openid = resolveOpenid(child);
            if (!StringUtils.hasText(openid)) {
                saveRecord(task, storeId, child, CHANNEL_WECHAT, wechatRecipient(null), message,
                        false, BigDecimal.ZERO, null, "家长未绑定微信公众号");
                outcomes.add("微信：家长未绑定公众号");
            } else {
                SendOutcome outcome = wechatSender.send(openid, new WechatChannelSender.TemplateValues(
                        message.title(), child.getNameMask(), message.content(),
                        storeName, message.timeText(), message.statusText()), wechatRuntime);
                String recipient = wechatRecipient(openid);
                if (outcome.success()) {
                    successCount++;
                    saveRecord(task, storeId, child, CHANNEL_WECHAT, recipient, message,
                            true, BigDecimal.ZERO, outcome.remark(), null);
                    outcomes.add("微信：发送成功");
                } else {
                    saveRecord(task, storeId, child, CHANNEL_WECHAT, recipient, message,
                            false, BigDecimal.ZERO, null, outcome.failReason());
                    outcomes.add("微信：发送失败");
                }
            }
        }

        int status = successCount > 0 ? TASK_DONE : TASK_FAILED;
        taskMapper.finish(task.getId(), status, retryCount(task), truncate(String.join("；", outcomes), 200));
    }

    /** 异常兜底：未产生任何发送记录才允许重试，避免重复计费 */
    private void handleUnexpected(NotifyTask task, Exception e) {
        int retryCount = retryCount(task);
        String reason = truncate("处理异常：" + e.getMessage(), 200);
        if (retryCount < maxRetry && recordService.countByTask(task.getId()) == 0) {
            taskMapper.requeue(task.getId(), retryCount + 1, reason);
            return;
        }
        taskMapper.finish(task.getId(), TASK_FAILED, retryCount, reason);
    }

    private void saveRecord(NotifyTask task, Long storeId, NotifySourceMapper.ChildContact child, int channel,
                            String recipient, NotifyMessageRenderer.RenderedMessage message,
                            boolean success, BigDecimal fee, String remark, String failReason) {
        NotifyRecord record = new NotifyRecord();
        record.setStoreId(storeId);
        record.setTaskId(task.getId());
        record.setEventType(task.getEventType());
        record.setChannel(channel);
        record.setChildId(child.getId());
        record.setChildName(child.getNameMask());
        record.setRecipient(recipient);
        record.setTitle(message.title());
        record.setContent(message.content());
        record.setStatus(success ? 1 : 0);
        record.setFee(fee);
        record.setRemark(remark);
        record.setFailReason(failReason);
        record.setSentAt(LocalDateTime.now());
        recordService.save(record);
    }

    /** 家长手机号：优先家长账号手机号，回退档案登记的监护人手机号（均需可解密） */
    private String resolveParentPhone(NotifySourceMapper.ChildContact child) {
        if (child.getParentUserId() != null) {
            Map<String, Object> user = sourceMapper.selectUserById(child.getParentUserId());
            String phone = user == null ? null : asText(user.get("phone"));
            if (StringUtils.hasText(phone) && phone.matches("^1[3-9]\\d{9}$")) {
                return phone;
            }
        }
        String decrypted = cipherHelper.decrypt(child.getPhoneEncrypted());
        if (StringUtils.hasText(decrypted) && decrypted.matches("^1[3-9]\\d{9}$")) {
            return decrypted;
        }
        return null;
    }

    private String resolveOpenid(NotifySourceMapper.ChildContact child) {
        if (child.getParentUserId() == null) {
            return null;
        }
        Map<String, Object> user = sourceMapper.selectUserById(child.getParentUserId());
        return user == null ? null : asText(user.get("wechatOpenid"));
    }

    private String wechatRecipient(String openid) {
        if (!StringUtils.hasText(openid)) {
            return "未绑定";
        }
        return "微信-" + (openid.length() <= 6 ? openid : openid.substring(openid.length() - 6));
    }

    private boolean isOn(Integer flag) {
        return flag != null && flag == 1;
    }

    private int retryCount(NotifyTask task) {
        return task.getRetryCount() == null ? 0 : task.getRetryCount();
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
