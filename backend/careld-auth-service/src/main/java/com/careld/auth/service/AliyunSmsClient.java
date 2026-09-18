package com.careld.auth.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.careld.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信发送（dysmsapi）
 */
@Slf4j
@Component
public class AliyunSmsClient {

    private static final String ENDPOINT = "dysmsapi.aliyuncs.com";

    /**
     * 发送验证码短信；阿里云返回非 OK 或调用异常时抛出带具体原因的 BusinessException
     */
    public void sendVerifyCode(String phone, String code, SmsConfigService.SmsRuntimeConfig runtime) {
        try {
            Config config = new Config()
                    .setAccessKeyId(runtime.accessKeyId())
                    .setAccessKeySecret(runtime.accessKeySecret())
                    .setEndpoint(ENDPOINT);
            Client client = new Client(config);

            String paramName = runtime.templateParam() == null || runtime.templateParam().isBlank()
                    ? "code" : runtime.templateParam();
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(runtime.signName())
                    .setTemplateCode(runtime.templateCode())
                    .setTemplateParam("{\"" + paramName + "\":\"" + code + "\"}");

            RuntimeOptions runtimeOptions = new RuntimeOptions()
                    .setConnectTimeout(5000)
                    .setReadTimeout(5000);

            SendSmsResponse response = client.sendSmsWithOptions(request, runtimeOptions);
            String respCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equals(respCode)) {
                String message = response.getBody() == null ? "无响应" : response.getBody().getMessage();
                log.error("阿里云短信发送失败 phone={} code={} message={}", phone, respCode, message);
                throw new BusinessException(1005, "短信发送失败：" + respCode + " " + message);
            }
            log.info("[SMS] 阿里云短信发送成功 phone={} template={}", phone, runtime.templateCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里云短信发送异常 phone={}", phone, e);
            throw new BusinessException(1005, "短信发送失败：" + e.getMessage());
        }
    }
}
