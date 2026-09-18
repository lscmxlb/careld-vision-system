package com.careld.schedule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * date 精确匹配某天；date 为空且 startDate 非空时按"不早于 startDate"过滤
     * keyword 按儿童姓名/家长手机号模糊匹配（解密后内存过滤）
     * statuses 多状态过滤（1-5，5=已爽约）；非空时优先于 status/noShowFlag
     * noShowFlag=true 仅返回爽约记录；false 仅返回非爽约记录（"已取消"筛选需排除爽约）；null 不过滤
     */
    IPage<ReserveOrder> listReserves(Long storeId, Long childId, Integer status, List<Integer> statuses, Boolean noShowFlag, LocalDate date, LocalDate startDate, String keyword, Integer page, Integer size);

    /**
     * 预约详情
     */
    ReserveOrder getReserveById(Long id);

    /**
     * 更新排班
     */
    void updateSchedule(Long id, Schedule schedule);

    /**
     * 删除排班（逻辑删除）
     */
    void deleteSchedule(Long id);

    /**
     * 批量创建排班（按星期过滤）
     */
    void batchCreate(com.careld.schedule.dto.BatchScheduleRequest request);

    /**
     * 创建预约（新链路：slotId 指定时段时走养护预约流程，校验审核/次数/满额/每日一约并扣减次数）
     */
    Long createReserveV2(com.careld.schedule.entity.ReserveOrder order);

    /**
     * 开始养护：状态 1→2，同事务创建养护记录并录入养护前视力
     * params: visionBeforeLeft / visionBeforeRight / visionBeforeBoth / startTime(HH:mm) / executorId / executorName
     */
    void startCare(Long id, java.util.Map<String, String> params);

    /**
     * 完成养护：状态 2→3，录入养护后视力
     * params: visionAfterLeft / visionAfterRight / visionAfterBoth
     */
    void completeCare(Long id, java.util.Map<String, String> params);

    /** 查询预约的养护记录（养护记录登记弹窗回填用） */
    com.careld.schedule.entity.CareRecord getCareRecord(Long reserveId);

    /**
     * 标记爽约：状态 1→4，标记 no_show_flag=1，不退还预约次数（与取消不同）
     */
    void markNoShow(Long id);

    /**
     * 系统自动标记爽约：时段结束超过配置时长仍未处理的预约，由定时任务调用（不退还次数，操作人记为"系统"）
     */
    void autoMarkNoShow(Long id);

    /**
     * 预约调整：已预约记录更换到新的可约时段（释放旧时段名额、占用新时段，不扣减/退还次数）
     */
    void adjustReserve(Long id, Long slotId);

    /**
     * 预约统计：起止日期内每日预约数与完成数
     */
    java.util.List<java.util.Map<String, Object>> statisticsReserves(Long storeId, LocalDate startDate, LocalDate endDate);
}
