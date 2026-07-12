package com.careld.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.careld.common.exception.BusinessException;
import com.careld.schedule.entity.ReserveOrder;
import com.careld.schedule.entity.Schedule;
import com.careld.schedule.mapper.ReserveOrderMapper;
import com.careld.schedule.mapper.ScheduleMapper;
import com.careld.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final ReserveOrderMapper reserveOrderMapper;

    @Override
    public Long createSchedule(Schedule schedule) {
        schedule.setReservedCount(0);
        schedule.setStatus(1);
        scheduleMapper.insert(schedule);
        return schedule.getId();
    }

    @Override
    @Transactional
    public void batchCreateSchedule(LocalDate startDate, LocalDate endDate, Long technicianId, List<Schedule> timeSlots) {
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (Schedule slot : timeSlots) {
                Schedule schedule = new Schedule();
                schedule.setStoreId(slot.getStoreId());
                schedule.setScheduleDate(date);
                schedule.setTechnicianId(technicianId);
                schedule.setTimeSlotStart(slot.getTimeSlotStart());
                schedule.setTimeSlotEnd(slot.getTimeSlotEnd());
                schedule.setMaxCapacity(slot.getMaxCapacity());
                createSchedule(schedule);
            }
        }
    }

    @Override
    public List<Schedule> getCalendar(Long storeId, LocalDate startDate, LocalDate endDate) {
        return scheduleMapper.selectByDateRange(storeId, startDate, endDate);
    }

    @Override
    @Transactional
    public Long createReserve(ReserveOrder order) {
        Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
        if (schedule == null) {
            throw new BusinessException(404, "排班不存在");
        }
        if (schedule.getReservedCount() >= schedule.getMaxCapacity()) {
            throw new BusinessException(5002, "预约已满");
        }

        // 生成订单号
        order.setOrderNo("R" + System.currentTimeMillis());
        // 状态：1=待到店（与前端约定一致）
        order.setStatus(1);
        order.setReserveDate(schedule.getScheduleDate());
        order.setReserveTimeStart(schedule.getTimeSlotStart());
        order.setReserveTimeEnd(schedule.getTimeSlotEnd());
        order.setStoreId(schedule.getStoreId());

        // 插入预约订单
        reserveOrderMapper.insert(order);

        // 递增已预约数
        scheduleMapper.incrementReserved(schedule.getId());

        return order.getId();
    }

    @Override
    @Transactional
    public void cancelReserve(Long id, String reason) {
        ReserveOrder order = reserveOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "预约不存在");
        }
        order.setStatus(5);
        order.setCancelReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        reserveOrderMapper.updateById(order);

        // 释放排班名额
        scheduleMapper.decrementReserved(order.getScheduleId());
    }

    @Override
    @Transactional
    public void completeReserve(Long id) {
        ReserveOrder order = reserveOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "预约不存在");
        }
        order.setStatus(4);
        order.setCompletedAt(LocalDateTime.now());
        reserveOrderMapper.updateById(order);
    }

    @Override
    public List<ReserveOrder> listReserves(Long storeId, Long childId, Integer status, LocalDate date) {
        LambdaQueryWrapper<ReserveOrder> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(ReserveOrder::getStoreId, storeId);
        }
        if (childId != null) {
            wrapper.eq(ReserveOrder::getChildId, childId);
        }
        if (status != null) {
            wrapper.eq(ReserveOrder::getStatus, status);
        }
        if (date != null) {
            wrapper.eq(ReserveOrder::getReserveDate, date);
        }
        wrapper.orderByDesc(ReserveOrder::getReserveDate);
        List<ReserveOrder> orders = reserveOrderMapper.selectList(wrapper);

        // 从排班表补充 technicianName（storeName 由门店服务提供，此处暂不填充）
        for (ReserveOrder order : orders) {
            Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
            if (schedule != null) {
                order.setTechnicianName(schedule.getTechnicianName());
            }
        }
        return orders;
    }
}
