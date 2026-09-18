package com.careld.schedule.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.careld.schedule.entity.AppointmentConfig;
import com.careld.schedule.entity.ReserveOrder;
import com.careld.schedule.mapper.AppointmentConfigMapper;
import com.careld.schedule.mapper.CareRecordMapper;
import com.careld.schedule.mapper.ReserveOrderMapper;
import com.careld.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约自动流转任务：
 * 1. 每 5 分钟扫描「养护中」且开始时间超过配置时长（默认 1.5 小时）的预约，自动置为已完成
 * 2. 每 5 分钟扫描「已预约」且时段结束超过配置时长（autoNoShowHours，默认 12 小时）的预约，自动标记爽约（不退还次数）
 * 3. 未超过自动标记时长的爽约由医生在预约列表中手动标记并选择是否退还次数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveAutoTask {

    private final ReserveOrderMapper reserveOrderMapper;
    private final AppointmentConfigMapper configMapper;
    private final CareRecordMapper careRecordMapper;
    private final ScheduleService scheduleService;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void autoComplete() {
        List<ReserveOrder> list = reserveOrderMapper.selectList(
                new LambdaQueryWrapper<ReserveOrder>().eq(ReserveOrder::getStatus, 2));
        LocalDateTime now = LocalDateTime.now();
        for (ReserveOrder order : list) {
            if (order.getStartTime() == null) {
                continue;
            }
            AppointmentConfig config = configMapper.selectByStoreId(order.getStoreId());
            double hours = config == null || config.getAutoCompleteHours() == null
                    ? 1.5 : config.getAutoCompleteHours().doubleValue();
            if (order.getStartTime().plusMinutes((long) (hours * 60)).isBefore(now)) {
                order.setStatus(3);
                order.setCompletedAt(now);
                reserveOrderMapper.updateById(order);
                // 养护后视力留空，超时自动完成
                careRecordMapper.completeCare(order.getId(), null, null, null);
                log.info("预约 {} 超过 {} 小时未录入养护后视力，自动完成", order.getOrderNo(), hours);
            }
        }
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 90_000)
    public void autoMarkNoShow() {
        List<ReserveOrder> list = reserveOrderMapper.selectList(
                new LambdaQueryWrapper<ReserveOrder>().eq(ReserveOrder::getStatus, 1));
        LocalDateTime now = LocalDateTime.now();
        for (ReserveOrder order : list) {
            if (order.getReserveDate() == null || order.getReserveTimeEnd() == null) {
                continue;
            }
            AppointmentConfig config = configMapper.selectByStoreId(order.getStoreId());
            int hours = config == null || config.getAutoNoShowHours() == null
                    ? 12 : config.getAutoNoShowHours();
            LocalDateTime deadline = LocalDateTime.of(order.getReserveDate(), order.getReserveTimeEnd())
                    .plusHours(hours);
            if (deadline.isBefore(now)) {
                try {
                    scheduleService.autoMarkNoShow(order.getId());
                    log.info("预约 {} 时段结束超过 {} 小时未处理，自动标记爽约", order.getOrderNo(), hours);
                } catch (Exception e) {
                    log.warn("预约 {} 自动标记爽约失败：{}", order.getOrderNo(), e.getMessage());
                }
            }
        }
    }
}
