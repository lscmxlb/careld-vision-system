package com.careld.schedule.service;

import com.careld.schedule.entity.ReserveOrder;
import com.careld.schedule.entity.Schedule;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {
    Long createSchedule(Schedule schedule);
    void batchCreateSchedule(LocalDate startDate, LocalDate endDate, Long technicianId, List<Schedule> timeSlots);
    List<Schedule> getCalendar(Long storeId, LocalDate startDate, LocalDate endDate);
    Long createReserve(ReserveOrder order);
    void cancelReserve(Long id, String reason);
    void completeReserve(Long id);
    /**
     * 查询预约列表。storeId 为空时返回所有记录（家长端按 childId 过滤）。
     */
    List<ReserveOrder> listReserves(Long storeId, Long childId, Integer status, LocalDate date);
}
