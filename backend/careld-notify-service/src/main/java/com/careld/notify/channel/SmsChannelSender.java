package com.careld.notify.channel;

import com.alibaba.fastjson2.JSON;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.careld.common.utils.MaskUtil;
import com.careld.notify.service.NotifyChannelRuntimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 短信通道发送（阿里云 dysmsapi）
 *
 * <p>未配置真实凭据时按「模拟通道」返回成功，由调用方照常计费并备注「模拟通道」。</p>
 */
@Slf4j
@Component
public class SmsChannelSender {

    private static final String ENDPOINT = "dysmsapi.aliyuncs.com";

    public SendOutcome send(String phone, String eventType, Map<String, String> vars, String content,
                            NotifyChannelRuntimeService.SmsRuntime runtime) {
        if (!runtime.realSendReady()) {
            log.info("[SMS][MOCK] phone={} template={} params={}",
                    MaskUtil.maskPhone(phone), runtime.noticeTemplateCode(),
                    buildTemplateParams(vars, content, runtime));
            return SendOutcome.success(NotifyChannelRuntimeService.MOCK_REMARK);
        }
        if (!runtime.appliesTo(eventType)) {
            return SendOutcome.failure("该事件（" + eventType + "）未配置对应的短信模板，已停发");
        }
        List<String> missing = new ArrayList<>();
        Map<String, String> params = mapParams(vars, content, runtime, missing);
        if (!missing.isEmpty()) {
            return SendOutcome.failure("通知模板变量缺少取值：" + String.join("、", missing));
        }
        String templateParamJson = JSON.toJSONString(params);
        try {
            Config config = new Config()
                    .setAccessKeyId(runtime.accessKeyId())
                    .setAccessKeySecret(runtime.accessKeySecret())
                    .setEndpoint(ENDPOINT);
            Client client = new Client(config);

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(runtime.signName())
                    .setTemplateCode(runtime.noticeTemplateCode())
                    .setTemplateParam(templateParamJson);

            RuntimeOptions runtimeOptions = new RuntimeOptions()
                    .setConnectTimeout(5000)
                    .setReadTimeout(5000);

            SendSmsResponse response = client.sendSmsWithOptions(request, runtimeOptions);
            String respCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equals(respCode)) {
                String message = response.getBody() == null ? "无响应" : response.getBody().getMessage();
                log.error("[SMS] 阿里云短信发送失败 phone={} code={} message={} params={}",
                        MaskUtil.maskPhone(phone), respCode, message, templateParamJson);
                return SendOutcome.failure("短信发送失败：" + respCode + " " + message);
            }
            log.info("[SMS] 阿里云短信发送成功 phone={} template={} params={}",
                    MaskUtil.maskPhone(phone), runtime.noticeTemplateCode(), templateParamJson);
            return SendOutcome.success("阿里云短信");
        } catch (Exception e) {
            log.error("[SMS] 阿里云短信发送异常 phone={} params={}", MaskUtil.maskPhone(phone), templateParamJson, e);
            return SendOutcome.failure("短信发送异常：" + e.getMessage());
        }
    }

    /** 构造阿里云模板参数 JSON（供配置预览，不实际发送） */
    public String buildTemplateParams(Map<String, String> vars, String content, NotifyChannelRuntimeService.SmsRuntime runtime) {
        return JSON.toJSONString(mapParams(vars, content, runtime, null));
    }

    /**
     * 按「模板变量名 ← 变量语义」映射取值
     *
     * @param missing 非空时收集缺值项（发送前拦截）；为 null 时缺值置空串（仅用于预览）
     */
    private Map<String, String> mapParams(Map<String, String> vars, String content,
                                          NotifyChannelRuntimeService.SmsRuntime runtime, List<String> missing) {
        List<String> fields = runtime.fieldList();
        List<String> roles = runtime.roleList();
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            String role = i < roles.size() ? roles.get(i) : "content";
            String value = vars == null ? null : vars.get(role);
            if (!StringUtils.hasText(value) && "content".equals(role)) {
                value = content;
            }
            if (!StringUtils.hasText(value)) {
                if (missing != null) {
                    missing.add(fields.get(i) + "(" + role + ")");
                }
                params.put(fields.get(i), "");
                continue;
            }
            params.put(fields.get(i), value);
        }
        return params;
    }
}
