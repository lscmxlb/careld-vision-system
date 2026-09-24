package com.careld.notify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.careld.notify.channel.WechatChannelSender;
import com.careld.notify.dto.WechatBindQrVO;
import com.careld.notify.entity.WechatBindTicket;
import com.careld.notify.mapper.NotifySourceMapper;
import com.careld.notify.mapper.WechatBindTicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信公众号扫码绑定
 *
 * <p>家长端展示带参二维码（scene 随机、10 分钟有效），家长扫码关注后微信把
 * subscribe / SCAN 事件推到 {@code /api/v1/notify/wechat/callback}（公众平台
 * 「服务器配置」指向本服务，消息加解密方式用明文模式），按 scene 找回票据并
 * 写入 sys_user.wechat_openid，家长端轮询状态即显示「已绑定」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatBindService {

    public static final String KEY_SERVER_TOKEN = "wechat.mp.serverToken";
    public static final String KEY_PUBLIC_BASE_URL = "wechat.mp.publicBaseUrl";
    public static final String CALLBACK_PATH = "/api/v1/notify/wechat/callback";

    private static final String GROUP = "wechat";
    private static final String DEFAULT_PUBLIC_BASE_URL = "http://39.162.49.28";
    private static final int EXPIRE_SECONDS = 600;
    private static final String SUBSCRIBE_SCENE_PREFIX = "qrscene_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WechatBindTicketMapper ticketMapper;
    private final NotifySourceMapper sourceMapper;
    private final NotifyChannelRuntimeService runtimeService;
    private final WechatChannelSender wechatSender;

    /** 生成（或复用未过期的）带参二维码；凭据未配置时返回 configured=false 供前端降级 */
    public WechatBindQrVO createBindQr(Long userId) {
        WechatBindQrVO vo = new WechatBindQrVO();
        vo.setExpireSeconds(EXPIRE_SECONDS);

        NotifyChannelRuntimeService.WechatRuntime runtime = runtimeService.loadWechatRuntime();
        if (!StringUtils.hasText(runtime.appId()) || !StringUtils.hasText(runtime.appSecret())) {
            vo.setConfigured(false);
            vo.setHint("公众号凭据尚未配置（总部在管理后台「系统设置 → 微信通知配置」填写后，这里会显示真实的关注二维码）");
            return vo;
        }

        Map<String, Object> tokenResponse = wechatSender.token(runtime);
        Object token = tokenResponse.get("access_token");
        if (token == null) {
            vo.setConfigured(false);
            vo.setHint("获取 access_token 失败：" + tokenResponse.get("errcode") + " " + tokenResponse.get("errmsg"));
            return vo;
        }

        // 同一家长复用未过期的票据，避免重复生成二维码
        WechatBindTicket ticket = ticketMapper.selectOne(new LambdaQueryWrapper<WechatBindTicket>()
                .eq(WechatBindTicket::getUserId, userId)
                .eq(WechatBindTicket::getStatus, 0)
                .gt(WechatBindTicket::getExpireAt, LocalDateTime.now())
                .orderByDesc(WechatBindTicket::getId)
                .last("LIMIT 1"));
        if (ticket == null) {
            ticket = new WechatBindTicket();
            ticket.setScene(randomScene());
            ticket.setUserId(userId);
            ticket.setStatus(0);
            ticket.setExpireAt(LocalDateTime.now().plusSeconds(EXPIRE_SECONDS));
            ticketMapper.insert(ticket);
        }

        Map<String, Object> qrResponse = wechatSender.rawCreateQrCode(token.toString(), ticket.getScene(), EXPIRE_SECONDS);
        Object qrTicket = qrResponse.get("ticket");
        if (qrTicket == null) {
            vo.setConfigured(false);
            vo.setHint("创建带参二维码失败：" + qrResponse.get("errcode") + " " + qrResponse.get("errmsg")
                    + "（未认证公众号无此接口权限，需使用已认证公众号）");
            return vo;
        }
        vo.setConfigured(true);
        vo.setScene(ticket.getScene());
        vo.setQrImageUrl(WechatChannelSender.qrImageUrl(qrTicket.toString()));
        return vo;
    }

    /** 服务器配置校验（GET）：签名通过原样回显 echostr */
    public boolean verifySignature(String signature, String timestamp, String nonce) {
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)) {
            return false;
        }
        String[] parts = {serverToken(), timestamp, nonce};
        java.util.Arrays.sort(parts);
        String raw = String.join("", parts);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.warn("[WECHAT-BIND] 签名计算失败: {}", e.getMessage());
            return false;
        }
    }

    /** 处理微信推送的事件（明文模式 XML），返回给微信的应答体 */
    public String handleEvent(String xml) {
        Map<String, String> message = parseXml(xml);
        String msgType = message.get("MsgType");
        String openid = message.get("FromUserName");
        String event = message.get("Event");
        String eventKey = message.get("EventKey");
        log.info("[WECHAT-BIND] 收到微信事件 msgType={} event={} eventKey={} openid={}", msgType, event, eventKey, tail(openid));

        if (!StringUtils.hasText(openid) || !"event".equalsIgnoreCase(msgType) || !StringUtils.hasText(event)) {
            return "success";
        }
        if ("subscribe".equalsIgnoreCase(event) || "SCAN".equalsIgnoreCase(event)) {
            String scene = eventKey == null ? "" : eventKey;
            if (scene.startsWith(SUBSCRIBE_SCENE_PREFIX)) {
                scene = scene.substring(SUBSCRIBE_SCENE_PREFIX.length());
            }
            if (StringUtils.hasText(scene)) {
                bindByScene(scene, openid);
            }
        } else if ("unsubscribe".equalsIgnoreCase(event)) {
            int cleared = sourceMapper.unbindByOpenid(openid);
            log.info("[WECHAT-BIND] 取关解绑 openid={} 影响行数={}", tail(openid), cleared);
        }
        return "success";
    }

    /** 网页授权拿到 openid 后绑定（家长端在微信内一键绑定走这里） */
    public void bindOpenid(Long userId, String openid) {
        Long occupied = sourceMapper.selectUserIdByOpenid(openid);
        if (occupied != null && !occupied.equals(userId)) {
            log.warn("[WECHAT-BIND] openid={} 原绑定用户 {}，改绑到 {}", tail(openid), occupied, userId);
            sourceMapper.unbindWechat(occupied);
        }
        sourceMapper.bindWechat(userId, openid);
        log.info("[WECHAT-BIND] 绑定成功 userId={} openid={}", userId, tail(openid));
    }

    private void bindByScene(String scene, String openid) {
        WechatBindTicket ticket = ticketMapper.selectOne(new LambdaQueryWrapper<WechatBindTicket>()
                .eq(WechatBindTicket::getScene, scene)
                .last("LIMIT 1"));
        if (ticket == null) {
            log.warn("[WECHAT-BIND] 未找到 scene={} 对应的绑定票据（可能二维码已失效或已被清理）", scene);
            return;
        }
        if (ticket.getExpireAt() != null && ticket.getExpireAt().isBefore(LocalDateTime.now())) {
            ticketMapper.update(null, new LambdaUpdateWrapper<WechatBindTicket>()
                    .eq(WechatBindTicket::getId, ticket.getId())
                    .set(WechatBindTicket::getStatus, 2));
            log.warn("[WECHAT-BIND] scene={} 二维码已过期（{}）", scene, ticket.getExpireAt());
            return;
        }
        bindOpenid(ticket.getUserId(), openid);
        ticketMapper.update(null, new LambdaUpdateWrapper<WechatBindTicket>()
                .eq(WechatBindTicket::getId, ticket.getId())
                .set(WechatBindTicket::getStatus, 1)
                .set(WechatBindTicket::getOpenid, openid));
    }

    /** 服务器配置的 Token（首次访问自动生成并落库，供公众平台填写） */
    public String serverToken() {
        String token = value(KEY_SERVER_TOKEN, "");
        if (!StringUtils.hasText(token)) {
            synchronized (this) {
                token = sourceMapper.selectConfigValue(KEY_SERVER_TOKEN);
                if (!StringUtils.hasText(token)) {
                    token = randomScene();
                    sourceMapper.upsertConfig(KEY_SERVER_TOKEN, token, GROUP, "公众号服务器配置 Token");
                }
            }
        }
        return token;
    }

    /** 重新生成服务器配置 Token（公众平台需要同步替换） */
    public String regenerateServerToken() {
        String token = randomScene();
        sourceMapper.upsertConfig(KEY_SERVER_TOKEN, token, GROUP, "公众号服务器配置 Token");
        return token;
    }

    public String publicBaseUrl() {
        return value(KEY_PUBLIC_BASE_URL, DEFAULT_PUBLIC_BASE_URL);
    }

    public void savePublicBaseUrl(String baseUrl) {
        sourceMapper.upsertConfig(KEY_PUBLIC_BASE_URL,
                StringUtils.hasText(baseUrl) ? trimTrailingSlash(baseUrl.trim()) : DEFAULT_PUBLIC_BASE_URL,
                GROUP, "公众号回调公网地址");
    }

    public String callbackUrl() {
        return publicBaseUrl() + CALLBACK_PATH;
    }

    private Map<String, String> parseXml(String xml) {
        Map<String, String> map = new HashMap<>();
        if (!StringUtils.hasText(xml)) {
            return map;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            for (String tag : new String[]{"MsgType", "Event", "EventKey", "FromUserName", "ToUserName"}) {
                NodeList nodes = document.getElementsByTagName(tag);
                if (nodes.getLength() > 0) {
                    map.put(tag, nodes.item(0).getTextContent());
                }
            }
        } catch (Exception e) {
            // 安全模式下报文是加密的，这里解析不出来；服务器配置需选「明文模式」
            log.warn("[WECHAT-BIND] 报文解析失败（若公众平台选了安全/兼容模式请改为明文模式）: {}", e.getMessage());
        }
        return map;
    }

    private String randomScene() {
        byte[] bytes = new byte[10];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder("b");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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

    private static String trimTrailingSlash(String text) {
        String result = text;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String tail(String openid) {
        if (!StringUtils.hasText(openid)) {
            return "-";
        }
        return openid.length() <= 6 ? openid : openid.substring(openid.length() - 6);
    }
}
