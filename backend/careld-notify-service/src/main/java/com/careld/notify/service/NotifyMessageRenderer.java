package com.careld.notify.service;

import com.alibaba.fastjson2.JSON;
import com.careld.common.entity.NotifyTask;
import com.careld.common.notify.NotifyEventTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通知文案渲染（短信正文与微信模板消息共用同一份标题/正文）
 */
@Slf4j
@Component
public class NotifyMessageRenderer {

    /**
     * 渲染结果
     *
     * @param vars 短信模板变量取值（键为语义角色：storeName/childName/noticeTitle/noticeStatus/
     *             noticeTime/reserveDate/timeRange/timeStart/timeEnd/reason/oldReserveDate/oldTimeRange/content），
     *             供多变量短信模板按配置顺序取值
     */
    public record RenderedMessage(String title, String content, String timeText, String statusText,
                                  Map<String, String> vars) {
    }

    public RenderedMessage render(NotifyTask task, String childName, String storeName) {
        return render(task, childName, storeName, null);
    }

    /**
     * @param childRealName 儿童真实姓名；短信变量用它（家长收到的短信按模板示例显示全名），
     *                      为空时回退脱敏姓名。站内记录正文始终用脱敏姓名。
     */
    public RenderedMessage render(NotifyTask task, String childName, String storeName, String childRealName) {
        Map<String, Object> payload = parsePayload(task.getPayload());
        String store = StringUtils.hasText(storeName) ? storeName : "本医院";
        String child = StringUtils.hasText(childName) ? childName : "您的孩子";
        String title = NotifyEventTypes.label(task.getEventType());
        String content = switch (task.getEventType() == null ? "" : task.getEventType()) {
            case NotifyEventTypes.CHILD_CREATED ->
                    "【" + store + "】" + child + " 已完成视力健康档案建档，我们会持续关注孩子的视力变化。";
            // 养护提醒与预约成功文案一致：预约开始前再次提醒家长按时到院
            case NotifyEventTypes.RESERVE_CREATED, NotifyEventTypes.CARE_REMINDER ->
                    "【" + store + "】" + child + " 已预约 " + slot(payload) + "，请按预约时间到院。";
            case NotifyEventTypes.RESERVE_CANCELLED ->
                    "【" + store + "】" + child + " " + slot(payload) + " 的预约已取消。"
                            + (StringUtils.hasText(text(payload.get("reason"))) ? "原因：" + text(payload.get("reason")) + "。" : "");
            case NotifyEventTypes.RESERVE_ADJUSTED ->
                    "【" + store + "】" + child + " 的预约已调整为 " + slot(payload) + "，请按新时间到院。";
            case NotifyEventTypes.CARE_COMPLETED ->
                    "【" + store + "】" + child + " 本次养护已完成"
                            + (StringUtils.hasText(text(payload.get("visionInfo"))) ? "，" + text(payload.get("visionInfo")) : "")
                            + "，请按医嘱进行家庭养护。";
            default -> "【" + store + "】" + child + " 的服务已办理完成。";
        };
        String timeText = templateTime(payload);
        String statusText = statusPhrase(task.getEventType());

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("storeName", store);
        vars.put("childName", StringUtils.hasText(childRealName) ? childRealName : child);
        vars.put("noticeTitle", title);
        vars.put("noticeStatus", statusText);
        vars.put("noticeTime", timeText);
        vars.put("content", content);
        vars.put("reserveDate", dateText(payload));
        vars.put("timeRange", timeRange(payload));
        vars.put("timeStart", text(payload.get("timeStart")));
        vars.put("timeEnd", text(payload.get("timeEnd")));
        vars.put("reason", text(payload.get("reason")));
        vars.put("oldReserveDate", dateText(text(payload.get("oldReserveDate"))));
        vars.put("oldTimeRange", timeRange(text(payload.get("oldTimeStart")), text(payload.get("oldTimeEnd"))));

        return new RenderedMessage(title, content, timeText, statusText, vars);
    }

    /** 模板「时间」字段：优先业务时间（预约日期+开始时间），格式化为「2026年9月25日 10:00」 */
    private String templateTime(Map<String, Object> payload) {
        String date = text(payload.get("reserveDate"));
        if (!StringUtils.hasText(date)) {
            return "";
        }
        String normalized = date.replace('-', '/');
        String[] parts = normalized.split("/");
        if (parts.length != 3) {
            return date;
        }
        String start = text(payload.get("timeStart"));
        String day = Integer.parseInt(parts[1]) + "月" + Integer.parseInt(parts[2]) + "日";
        return parts[0] + "年" + day + (StringUtils.hasText(start) ? " " + start : "");
    }

    /**
     * 预约日期文案：2026-9-25（短信模板 ${date} 的取值，年月日不补零）
     */
    private String dateText(Map<String, Object> payload) {
        return dateText(text(payload.get("reserveDate")));
    }

    private String dateText(String date) {
        if (!StringUtils.hasText(date)) {
            return "";
        }
        String[] parts = date.replace('-', '/').split("/");
        if (parts.length != 3) {
            return date;
        }
        return Integer.parseInt(parts[0]) + "-" + Integer.parseInt(parts[1]) + "-" + Integer.parseInt(parts[2]);
    }

    /** 预约时段文案：10:00-11:00（无开始时间时为空） */
    private String timeRange(Map<String, Object> payload) {
        return timeRange(text(payload.get("timeStart")), text(payload.get("timeEnd")));
    }

    private String timeRange(String start, String end) {
        if (!StringUtils.hasText(start)) {
            return "";
        }
        return start + (StringUtils.hasText(end) ? "-" + end : "");
    }

    /** 模板「状态」字段：短语控制在 5 个汉字以内 */
    private String statusPhrase(String eventType) {
        return switch (eventType == null ? "" : eventType) {
            case NotifyEventTypes.CHILD_CREATED -> "已建档";
            case NotifyEventTypes.RESERVE_CREATED, NotifyEventTypes.CARE_REMINDER -> "已预约";
            case NotifyEventTypes.RESERVE_CANCELLED -> "已取消";
            case NotifyEventTypes.RESERVE_ADJUSTED -> "已调整";
            case NotifyEventTypes.CARE_COMPLETED -> "已完成";
            default -> "已办理";
        };
    }

    private String slot(Map<String, Object> payload) {
        String date = text(payload.get("reserveDate"));
        String start = text(payload.get("timeStart"));
        String end = text(payload.get("timeEnd"));
        if (!StringUtils.hasText(date)) {
            return "预约时段";
        }
        if (!StringUtils.hasText(start)) {
            return date;
        }
        return date + " " + start + (StringUtils.hasText(end) ? "-" + end : "");
    }

    private Map<String, Object> parsePayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = JSON.parseObject(payload);
            return map == null ? new LinkedHashMap<>() : map;
        } catch (Exception e) {
            log.warn("通知任务载荷解析失败: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
