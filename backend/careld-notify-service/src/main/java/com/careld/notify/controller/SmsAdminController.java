package com.careld.notify.controller;

import com.careld.common.exception.BusinessException;
import com.careld.common.log.OperationLog;
import com.careld.common.notify.NotifyEventTypes;
import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.notify.channel.SendOutcome;
import com.careld.notify.channel.SmsChannelSender;
import com.careld.notify.dto.SmsTestRequest;
import com.careld.notify.service.NotifyChannelRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台「系统设置」：业务通知短信通道测试发送
 */
@Tag(name = "短信通知测试", description = "用已保存的阿里云凭据与通知模板真实发送一条样例通知短信")
@RestController
@RequestMapping("/api/v1/notify/sms/config")
@RequiredArgsConstructor
public class SmsAdminController {

    /** 测试短信正文（与真实通知同结构，便于核对模板变量渲染） */
    private static final String SAMPLE_CONTENT = "【示例医院】童童 已预约 2026-9-25 10:00-11:00，请按预约时间到院。（通道测试）";

    /** 样例通知的变量取值（对应预约成功事件） */
    private static final Map<String, String> SAMPLE_VARS = Map.ofEntries(
            Map.entry("storeName", "示例医院"),
            Map.entry("childName", "童童"),
            Map.entry("noticeTitle", "预约成功"),
            Map.entry("noticeStatus", "已预约"),
            Map.entry("noticeTime", "2026年9月25日 10:00"),
            Map.entry("content", SAMPLE_CONTENT),
            Map.entry("reserveDate", "2026-9-25"),
            Map.entry("timeRange", "10:00-11:00"),
            Map.entry("timeStart", "10:00"),
            Map.entry("timeEnd", "11:00"),
            Map.entry("reason", "医院调整或主动取消"),
            Map.entry("oldReserveDate", "2026-9-24"),
            Map.entry("oldTimeRange", "09:30-10:30"));

    private final SmsChannelSender smsChannelSender;
    private final NotifyChannelRuntimeService runtimeService;

    @OperationLog(module = "notify", action = "sms-test", description = "测试发送通知短信")
    @Operation(summary = "测试发送：dryRun=true 只预览模板变量取值，否则真实发送一条通知短信")
    @PostMapping("/test")
    @RequirePermission("settings:view")
    public Result<String> testSend(@Valid @RequestBody SmsTestRequest request) {
        String eventType = StringUtils.hasText(request.getEventType())
                ? request.getEventType().trim() : NotifyEventTypes.RESERVE_CREATED;
        if (!NotifyEventTypes.isValid(eventType)) {
            throw new BusinessException(400, "未知的通知事件类型：" + eventType);
        }
        // 按事件取模板：该事件单独配了模板 Code 时用它的变量映射
        NotifyChannelRuntimeService.SmsRuntime saved = runtimeService.loadSmsRuntimeFor(eventType);
        // 测试发送忽略「开启短信通知」开关，但要求凭据与通知模板齐全
        NotifyChannelRuntimeService.SmsRuntime runtime = new NotifyChannelRuntimeService.SmsRuntime(
                true, saved.accessKeyId(), saved.accessKeySecret(), saved.signName(),
                saved.noticeTemplateCode(), saved.noticeTemplateParam(),
                saved.noticeTemplateFields(), saved.noticeTemplateRoles(), saved.noticeTemplateEvents());
        if (Boolean.TRUE.equals(request.getDryRun())) {
            return Result.success(smsChannelSender.buildTemplateParams(SAMPLE_VARS, SAMPLE_CONTENT, runtime));
        }
        if (!runtime.realSendReady()) {
            throw new BusinessException(1005,
                    "请先保存阿里云 AccessKey ID/Secret、短信签名与通知模板 Code，再测试发送");
        }
        SendOutcome outcome = smsChannelSender.send(request.getPhone(), eventType,
                SAMPLE_VARS, SAMPLE_CONTENT, runtime);
        if (!outcome.success()) {
            throw new BusinessException(1005, outcome.failReason());
        }
        return Result.success(outcome.remark());
    }
}
