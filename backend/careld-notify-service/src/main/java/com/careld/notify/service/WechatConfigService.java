package com.careld.notify.service;

import com.careld.common.exception.BusinessException;
import com.careld.common.security.AesUtil;
import com.careld.notify.channel.WechatChannelSender;
import com.careld.notify.dto.WechatConfigRequest;
import com.careld.notify.dto.WechatConfigVO;
import com.careld.notify.dto.WechatProbeVO;
import com.careld.notify.dto.WechatTestRequest;
import com.careld.notify.mapper.NotifySourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信通知通道配置与联调（总部在管理后台维护，存于 sys_config）
 *
 * <p>凭据与模板齐全后业务链路自动切到真实发送；探针与测试发送接口用于联调期
 * 核对模板归属（模板消息 / 订阅通知）、模板字段名与微信返回的 errcode。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatConfigService {

    private static final String GROUP = "wechat";
    private static final Pattern IP_PATTERN = Pattern.compile("(\\d{1,3}(?:\\.\\d{1,3}){3})");
    private static final String DEFAULT_ACCOUNT_NAME = "可尔欧得视力养护";
    private static final List<String> ROLE_NAMES = NotifyChannelRuntimeService.FIELD_ROLE_NAMES;

    private final NotifySourceMapper sourceMapper;
    private final NotifyChannelRuntimeService runtimeService;
    private final WechatChannelSender wechatSender;
    private final WechatBindService bindService;

    @Value("${encryption.key:careld-vision-encrypt-key-32byte}")
    private String encryptionKey;

    public WechatConfigVO getConfig() {
        NotifyChannelRuntimeService.WechatRuntime runtime = runtimeService.loadWechatRuntime();
        WechatConfigVO vo = new WechatConfigVO();
        vo.setAppId(runtime.appId());
        String cipher = value(NotifyChannelRuntimeService.KEY_WECHAT_APP_SECRET, "");
        vo.setAppSecretConfigured(StringUtils.hasText(cipher));
        vo.setAppSecretMasked(StringUtils.hasText(cipher) ? mask(runtime.appSecret()) : "");
        vo.setTemplateId(runtime.templateId());
        vo.setTemplateType(runtime.templateType());
        vo.setFields(runtime.fields());
        vo.setFieldRoles(runtime.fieldRoles());
        vo.setAccountName(runtimeService.officialAccountName());
        vo.setRealSendReady(runtime.realSendReady());
        vo.setPublicBaseUrl(bindService.publicBaseUrl());
        vo.setCallbackUrl(bindService.callbackUrl());
        vo.setServerToken(bindService.serverToken());
        return vo;
    }

    /** 重新生成服务器配置 Token（公众平台「服务器配置」需同步替换） */
    public WechatConfigVO regenerateServerToken() {
        bindService.regenerateServerToken();
        return getConfig();
    }

    public void saveConfig(WechatConfigRequest request) {
        String appId = trim(request.getAppId());
        String templateId = trim(request.getTemplateId());
        String templateType = trim(request.getTemplateType());
        String fields = StringUtils.hasText(request.getFields())
                ? request.getFields().trim() : NotifyChannelRuntimeService.DEFAULT_WECHAT_FIELDS;
        String accountName = trim(request.getAccountName());

        if (StringUtils.hasText(templateType) && !"template".equals(templateType) && !"subscribe".equals(templateType)) {
            throw new BusinessException(1005, "模板类型只能是 template（模板消息）或 subscribe（订阅通知）");
        }
        List<String> fieldNames = new ArrayList<>();
        for (String name : fields.split(",")) {
            fieldNames.add(name.trim());
        }
        if (fieldNames.isEmpty() || fieldNames.size() > 6
                || fieldNames.stream().anyMatch(name -> !name.matches("[A-Za-z_][A-Za-z0-9_]*"))) {
            throw new BusinessException(1005, "字段映射需为 1-6 个合法字段名（逗号分隔），如 thing2,thing20,time44,phrase46");
        }
        List<String> fieldRoles = new ArrayList<>();
        if (StringUtils.hasText(request.getFieldRoles())) {
            for (String role : request.getFieldRoles().split(",")) {
                fieldRoles.add(role.trim());
            }
            if (fieldRoles.size() != fieldNames.size()
                    || fieldRoles.stream().anyMatch(role -> !ROLE_NAMES.contains(role))) {
                throw new BusinessException(1005,
                        "字段语义需与字段映射一一对应（数量一致），取值只能是 " + String.join("/", ROLE_NAMES));
            }
        } else {
            fieldRoles.addAll(NotifyChannelRuntimeService.DEFAULT_WECHAT_FIELD_ROLES_LIST);
        }

        save(NotifyChannelRuntimeService.KEY_WECHAT_APP_ID, appId);
        // 留空表示不修改已保存的 AppSecret
        if (StringUtils.hasText(request.getAppSecret())) {
            save(NotifyChannelRuntimeService.KEY_WECHAT_APP_SECRET,
                    AesUtil.encrypt(request.getAppSecret().trim(), encryptionKey));
        }
        save(NotifyChannelRuntimeService.KEY_WECHAT_TEMPLATE_ID, templateId);
        save(NotifyChannelRuntimeService.KEY_WECHAT_TEMPLATE_TYPE, templateType);
        save(NotifyChannelRuntimeService.KEY_WECHAT_FIELDS, String.join(",", fieldNames));
        save(NotifyChannelRuntimeService.KEY_WECHAT_FIELD_ROLES, String.join(",", fieldRoles));
        save(NotifyChannelRuntimeService.KEY_WECHAT_ACCOUNT_NAME,
                StringUtils.hasText(accountName) ? accountName : DEFAULT_ACCOUNT_NAME);
        if (StringUtils.hasText(request.getPublicBaseUrl())) {
            bindService.savePublicBaseUrl(request.getPublicBaseUrl().trim());
        }
        if (StringUtils.hasText(request.getServerToken())) {
            sourceMapper.upsertConfig(WechatBindService.KEY_SERVER_TOKEN, request.getServerToken().trim(),
                    GROUP, "公众号服务器配置 Token");
        }
    }

    /** 探针：取 token + 拉两类模板列表 + 判定当前模板归属与字段名 */
    public WechatProbeVO probe() {
        NotifyChannelRuntimeService.WechatRuntime runtime = runtimeService.loadWechatRuntime();
        WechatProbeVO vo = new WechatProbeVO();
        vo.setAppId(runtime.appId());
        vo.setConfiguredTemplateId(runtime.templateId());
        vo.setConfiguredTemplateType(runtime.templateType());
        vo.setTemplateMessageTemplates(List.of());
        vo.setSubscribeTemplates(List.of());

        if (!StringUtils.hasText(runtime.appId()) || !StringUtils.hasText(runtime.appSecret())) {
            vo.setTokenOk(false);
            vo.setTokenErrmsg("尚未配置 AppID / AppSecret");
            vo.setHint("请先在下方填写公众号 AppID 与 AppSecret 并保存");
            return vo;
        }

        Map<String, Object> tokenResponse = wechatSender.rawToken(runtime.appId(), runtime.appSecret());
        Object token = tokenResponse.get("access_token");
        if (token == null) {
            vo.setTokenOk(false);
            vo.setTokenErrcode(tokenResponse.get("errcode"));
            vo.setTokenErrmsg(String.valueOf(tokenResponse.get("errmsg")));
            vo.setHint(hintFor(tokenResponse.get("errcode"), String.valueOf(tokenResponse.get("errmsg"))));
            return vo;
        }
        vo.setTokenOk(true);

        fillFollowers(vo, token.toString());

        Map<String, Object> templateResponse = wechatSender.rawPrivateTemplates(token.toString());
        vo.setTemplateMessageTemplates(normalize(templateResponse.get("template_list"),
                "template_id", "templateId"));

        Map<String, Object> subscribeResponse = wechatSender.rawSubscribeTemplates(token.toString());
        vo.setSubscribeTemplates(normalize(subscribeResponse.get("data"),
                "priTmplId", "templateId"));

        vo.setMatchedType("none");
        for (Map<String, Object> item : vo.getTemplateMessageTemplates()) {
            if (matchTemplate(item, runtime.templateId())) {
                vo.setMatchedType("template");
                fillMatched(vo, item);
                return vo;
            }
        }
        for (Map<String, Object> item : vo.getSubscribeTemplates()) {
            if (matchTemplate(item, runtime.templateId())) {
                vo.setMatchedType("subscribe");
                fillMatched(vo, item);
                return vo;
            }
        }
        if (StringUtils.hasText(runtime.templateId())) {
            vo.setHint("两个模板列表里都没有 " + runtime.templateId() + "：请确认模板是否属于该公众号，或模板 ID 是否复制完整");
        } else {
            vo.setHint("尚未填写模板 ID");
        }
        return vo;
    }

    /** 测试发送：用真实配置发一条模板消息，返回微信原始响应 */
    public Map<String, Object> testSend(WechatTestRequest request) {
        NotifyChannelRuntimeService.WechatRuntime runtime = runtimeService.loadWechatRuntime();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("openid", request.getOpenid());
        result.put("appId", runtime.appId());
        result.put("templateId", runtime.templateId());
        result.put("templateType", StringUtils.hasText(runtime.templateType()) ? runtime.templateType() : "template");
        result.put("fields", runtime.fields());
        result.put("fieldRoles", runtime.fieldRoles());
        if (!runtime.realSendReady()) {
            result.put("errcode", -2);
            result.put("errmsg", "尚未配置完整的公众号凭据（AppID / AppSecret / 模板 ID），当前仍是模拟通道");
            result.put("_stage", "config");
            return result;
        }
        String content = StringUtils.hasText(request.getContent())
                ? request.getContent()
                : "【联调测试】视力养护通知通道测试，收到本条消息说明公众号模板消息已打通。";
        Map<String, Object> response = wechatSender.sendRaw(
                request.getOpenid().trim(),
                new WechatChannelSender.TemplateValues("预约成功", "测试儿童", content,
                        "联调测试医院", templateTimeText(), "已预约"),
                runtime);
        result.putAll(response);
        Object errcode = response.get("errcode");
        if (errcode != null && !"0".equals(String.valueOf(errcode))) {
            result.put("hint", hintFor(errcode, String.valueOf(response.get("errmsg"))));
        }
        return result;
    }

    private void fillMatched(WechatProbeVO vo, Map<String, Object> item) {
        vo.setMatchedTitle(str(item.get("title")));
        vo.setMatchedContent(str(item.get("content")));
        vo.setMatchedFields(WechatChannelSender.parseFieldNames(str(item.get("content"))));
    }

    /** 关注者 openid 列表：测试发送时可直接挑一个真实 openid，失败仅提示不影响探针其余结果 */
    private void fillFollowers(WechatProbeVO vo, String token) {
        vo.setFollowers(List.of());
        Map<String, Object> response = wechatSender.rawFollowers(token);
        Object errcode = response.get("errcode");
        if (errcode != null && !"0".equals(String.valueOf(errcode))) {
            vo.setFollowerHint("拉取关注者失败：" + str(errcode) + " " + str(response.get("errmsg")));
            return;
        }
        List<String> openids = new ArrayList<>();
        if (response.get("data") instanceof Map<?, ?> data && data.get("openid") instanceof List<?> list) {
            for (Object openid : list) {
                openids.add(String.valueOf(openid));
            }
        }
        vo.setFollowers(openids);
        vo.setFollowerTotal(response.get("total") instanceof Number total ? total.intValue() : openids.size());
    }

    /** 测试发送用的示例业务时间（订阅通知 time 字段要求「2026年9月25日 10:00」这类格式） */
    private String templateTimeText() {
        return java.time.LocalDate.now().plusDays(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日")) + " 10:00";
    }

    private boolean matchTemplate(Map<String, Object> item, String templateId) {
        return StringUtils.hasText(templateId) && templateId.equals(str(item.get("templateId")));
    }

    /** 把微信返回的模板列表统一出 templateId/title/content/example，同时保留原始字段 */
    private List<Map<String, Object>> normalize(Object rawList, String idKey, String idAlias) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (!(rawList instanceof List<?> list)) {
            return items;
        }
        for (Object element : list) {
            if (!(element instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            map.forEach((key, value) -> item.put(String.valueOf(key), value));
            if (!item.containsKey("templateId") && map.get(idKey) != null) {
                item.put("templateId", String.valueOf(map.get(idKey)));
            }
            if (map.get(idAlias) != null) {
                item.put("templateId", String.valueOf(map.get(idAlias)));
            }
            item.putIfAbsent("title", "");
            item.putIfAbsent("content", "");
            items.add(item);
        }
        return items;
    }

    private String hintFor(Object errcode, String errmsg) {
        String code = str(errcode);
        String message = errmsg == null ? "" : errmsg;
        return switch (code) {
            case "40164" -> {
                Matcher matcher = IP_PATTERN.matcher(message);
                String ip = matcher.find() ? matcher.group(1) : "本服务器公网 IP";
                yield "调用方 IP 未加入公众号白名单：请在 微信公众平台 → 设置与开发 → 基本配置 → IP白名单 中添加 " + ip + "，保存后重试";
            }
            case "40013" -> "AppID 无效：请核对公众平台「设置与开发 → 基本配置」中的 AppID";
            case "40001", "40125" -> "AppSecret 错误：请到公众平台重置 AppSecret 后重新保存";
            case "40037" -> "template_id 无效：该模板不属于此公众号，请在模板库中重新选用并复制模板 ID";
            case "47003" -> "模板字段与入参不匹配：请按探针解析出的字段名修正字段映射后再发送";
            case "40003" -> "openid 无效：请填写关注该公众号的真实 openid（可让家长先扫码关注后从公众号后台用户列表获取）";
            case "43004" -> "该用户未关注公众号；订阅通知还需用户先订阅该模板";
            case "43101" -> "用户拒绝接收消息：订阅通知需要用户先完成订阅授权";
            case "-1" -> "调用微信接口异常：" + message;
            default -> "";
        };
    }

    private void save(String key, String value) {
        sourceMapper.upsertConfig(key, value == null ? "" : value, GROUP, null);
    }

    private String value(String key, String defaultValue) {
        try {
            String value = sourceMapper.selectConfigValue(key);
            return StringUtils.hasText(value) ? value : defaultValue;
        } catch (Exception e) {
            log.warn("读取系统配置失败 key={}: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String mask(String secret) {
        if (!StringUtils.hasText(secret)) {
            return "";
        }
        if (secret.length() <= 4) {
            return "****";
        }
        return "****" + secret.substring(secret.length() - 4);
    }
}
