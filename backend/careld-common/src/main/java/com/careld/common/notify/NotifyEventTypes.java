package com.careld.common.notify;

import java.util.List;

/**
 * 通知类型（业务事件）编码与名称
 *
 * <p>编码写入 notify_task.event_type / notify_record.event_type，
 * 医院端「通知服务 → 通知类型」勾选值也使用同一套编码。</p>
 */
public final class NotifyEventTypes {

    /** 建档成功 */
    public static final String CHILD_CREATED = "child_created";
    /** 预约成功 */
    public static final String RESERVE_CREATED = "reserve_created";
    /** 预约取消 */
    public static final String RESERVE_CANCELLED = "reserve_cancelled";
    /** 预约调整 */
    public static final String RESERVE_ADJUSTED = "reserve_adjusted";
    /** 养护提醒（预约开始前再次发送预约成功短信，由定时任务产生） */
    public static final String CARE_REMINDER = "care_reminder";
    /** 养护完成 */
    public static final String CARE_COMPLETED = "care_completed";
    /** 其它服务 */
    public static final String OTHER = "other";

    /** 全部类型（顺序与医院端复选框一致） */
    public static final List<String> ALL = List.of(
            CHILD_CREATED, RESERVE_CREATED, RESERVE_CANCELLED, RESERVE_ADJUSTED, CARE_REMINDER, CARE_COMPLETED, OTHER);

    /** 默认勾选类型：六个业务通知，不含「其它服务」 */
    public static final String DEFAULT_ENABLED =
            CHILD_CREATED + "," + RESERVE_CREATED + "," + RESERVE_CANCELLED + "," + RESERVE_ADJUSTED
                    + "," + CARE_REMINDER + "," + CARE_COMPLETED;

    private NotifyEventTypes() {
    }

    public static String label(String code) {
        if (code == null) {
            return "服务通知";
        }
        return switch (code) {
            case CHILD_CREATED -> "建档成功";
            case RESERVE_CREATED -> "预约成功";
            case RESERVE_CANCELLED -> "预约取消";
            case RESERVE_ADJUSTED -> "预约调整";
            case CARE_REMINDER -> "养护提醒";
            case CARE_COMPLETED -> "养护完成";
            default -> "其它服务";
        };
    }

    public static boolean isValid(String code) {
        return ALL.contains(code);
    }
}
