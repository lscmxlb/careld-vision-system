package com.careld.schedule.task;

import com.careld.common.notify.NotifyEventTypes;
import com.careld.common.notify.NotifyTaskWriter;
import com.careld.schedule.mapper.ReserveOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 养护提醒任务：预约开始前 N 分钟（默认 90，系统参数 notice.careReminderMinutes）
 * 再推送一条「养护提醒」通知，短信文案与预约成功一致。
 *
 * <p>只推送任务，实际是否发送由通知服务按医院「通知服务 → 通知类型」勾选与通道开关决定。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveReminderTask {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String KEY_REMINDER_MINUTES = "notice.careReminderMinutes";
    private static final int DEFAULT_REMINDER_MINUTES = 90;
    /** 提醒时刻过去多久内仍补发：服务重启/短暂停摆不丢提醒，也不会翻出很久以前的预约补发 */
    private static final int CATCH_UP_MINUTES = 30;

    private final ReserveOrderMapper reserveOrderMapper;
    private final NotifyTaskWriter notifyTaskWriter;

    @Scheduled(fixedDelayString = "${careld.schedule.reminder-interval-ms:60000}",
            initialDelayString = "${careld.schedule.reminder-initial-delay-ms:45000}")
    public void remindUpcoming() {
        int minutes = reminderMinutes();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusMinutes(Math.max(0, minutes - CATCH_UP_MINUTES));
        LocalDateTime until = now.plusMinutes(minutes);
        List<ReserveOrderMapper.ReminderCandidate> candidates =
                reserveOrderMapper.selectReminderCandidates(from, until, minutes);
        if (candidates.isEmpty()) {
            return;
        }
        for (ReserveOrderMapper.ReminderCandidate candidate : candidates) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("reserveDate", String.valueOf(candidate.getReserveDate()));
            payload.put("timeStart", candidate.getReserveTimeStart() == null
                    ? null : candidate.getReserveTimeStart().format(HM));
            payload.put("timeEnd", candidate.getReserveTimeEnd() == null
                    ? null : candidate.getReserveTimeEnd().format(HM));
            notifyTaskWriter.push(candidate.getStoreId(), candidate.getChildId(),
                    NotifyEventTypes.CARE_REMINDER, candidate.getId(), payload);
            log.info("预约 {} {} {} 生成养护提醒任务（提前 {} 分钟）", candidate.getId(),
                    candidate.getReserveDate(), payload.get("timeStart"), minutes);
        }
    }

    /** 提前量：系统参数缺失或非法时按默认 90 分钟 */
    private int reminderMinutes() {
        String value = reserveOrderMapper.selectConfigValue(KEY_REMINDER_MINUTES);
        if (!StringUtils.hasText(value)) {
            return DEFAULT_REMINDER_MINUTES;
        }
        try {
            int minutes = Integer.parseInt(value.trim());
            return minutes > 0 ? minutes : DEFAULT_REMINDER_MINUTES;
        } catch (NumberFormatException e) {
            log.warn("系统参数 {} 取值非法：{}，按 {} 分钟处理", KEY_REMINDER_MINUTES, value, DEFAULT_REMINDER_MINUTES);
            return DEFAULT_REMINDER_MINUTES;
        }
    }
}
