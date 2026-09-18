package com.careld.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.common.security.UserContext;
import com.careld.schedule.dto.BatchScheduleRequest;
import com.careld.schedule.entity.AppointmentConfig;
import com.careld.schedule.entity.CareRecord;
import com.careld.schedule.entity.ReserveOrder;
import com.careld.schedule.entity.Schedule;
import com.careld.schedule.entity.ScheduleSlot;
import com.careld.schedule.mapper.AppointmentConfigMapper;
import com.careld.schedule.mapper.CareRecordMapper;
import com.careld.schedule.mapper.OperatorMapper;
import com.careld.schedule.mapper.QuotaMapper;
import com.careld.schedule.mapper.ReserveOrderMapper;
import com.careld.schedule.mapper.ScheduleMapper;
import com.careld.schedule.mapper.ScheduleSlotMapper;
import com.careld.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final ReserveOrderMapper reserveOrderMapper;
    private final ScheduleSlotMapper slotMapper;
    private final QuotaMapper quotaMapper;
    private final CareRecordMapper careRecordMapper;
    private final AppointmentConfigMapper configMapper;
    private final OperatorMapper operatorMapper;

    @Value("${encryption.key:careld-vision-encrypt-key-32byte}")
    private String aesKey;

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

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
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(400, "请填写取消原因");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(4004, "当前状态不可取消");
        }
        // 取消时间窗：家长（user_type=3）距开始不足 N 小时不可取消；医院侧预约开始前可取消
        Integer userType = UserContext.getCurrentUserType();
        AppointmentConfig config = configMapper.selectByStoreId(order.getStoreId());
        int parentHours = config == null || config.getParentCancelHours() == null ? 24 : config.getParentCancelHours();
        LocalDateTime startDateTime = LocalDateTime.of(order.getReserveDate(), order.getReserveTimeStart());
        LocalDateTime now = LocalDateTime.now();
        if (userType != null && userType == 3) {
            if (now.isAfter(startDateTime.minusHours(parentHours))) {
                throw new BusinessException(4005, "距预约开始不足 " + parentHours + " 小时，不可取消");
            }
        } else if (!now.isBefore(startDateTime)) {
            throw new BusinessException(4006, "预约已开始，不可取消，请使用开始养护或爽约处理");
        }
        order.setStatus(4);
        order.setCancelReason(reason);
        order.setCancelledAt(now);
        // 记录取消操作人账户（与创建预约同一套姓名解析规则）
        order.setCancelOperatorId(UserContext.getCurrentUserId());
        String cancelUsername = UserContext.get() == null ? null : UserContext.get().getUsername();
        if (StringUtils.hasText(cancelUsername)) {
            String cancelName;
            if (Integer.valueOf(6).equals(UserContext.getCurrentUserType())) {
                cancelName = operatorMapper.selectStaffName(cancelUsername);
            } else {
                cancelName = operatorMapper.selectSysUserRealName(cancelUsername);
            }
            order.setCancelOperatorName(StringUtils.hasText(cancelName) ? cancelName : cancelUsername);
        }
        reserveOrderMapper.updateById(order);

        if (order.getSlotId() != null) {
            // 新链路：释放时段名额并自动退还次数
            slotMapper.decrementBooked(order.getSlotId());
            quotaMapper.changeRemaining(order.getChildId(), 1);
            quotaMapper.insertQuotaRecord(order.getChildId(), order.getStoreId(), 3, 1,
                    order.getId(), UserContext.getCurrentUserId(), "取消预约退还");
        } else {
            // 旧链路（技师排班）：仅释放名额
            scheduleMapper.decrementReserved(order.getScheduleId());
        }
    }

    @Override
    @Transactional
    public void completeReserve(Long id) {
        ReserveOrder order = reserveOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "预约不存在");
        }
        order.setStatus(3);
        order.setCompletedAt(LocalDateTime.now());
        reserveOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public Long createReserveV2(ReserveOrder order) {
        if (order.getChildId() == null || order.getSlotId() == null) {
            throw new BusinessException(400, "儿童与预约时段不能为空");
        }
        ScheduleSlot slot = slotMapper.selectById(order.getSlotId());
        if (slot == null || slot.getStatus() == null || slot.getStatus() != 1) {
            throw new BusinessException(4007, "预约时段不存在或已关闭");
        }
        if (LocalDateTime.of(slot.getSlotDate(), slot.getSlotStartTime()).isBefore(LocalDateTime.now())) {
            throw new BusinessException(4008, "只能预约当前时间之后的时段");
        }
        // 前置条件两层门槛：档案已审核 + 剩余次数 > 0
        Integer auditStatus = quotaMapper.selectAuditStatus(order.getChildId());
        if (auditStatus == null) {
            throw new BusinessException(404, "儿童档案不存在");
        }
        if (auditStatus != 1) {
            throw new BusinessException(4009, "档案未通过审核，暂不可预约");
        }
        // 禁用档案禁止预约（status: 1=启用, 0=禁用）
        Integer childStatus = quotaMapper.selectChildStatus(order.getChildId());
        if (childStatus == null) {
            throw new BusinessException(404, "儿童档案不存在");
        }
        if (childStatus != 1) {
            throw new BusinessException(4012, "档案已禁用，暂不可预约");
        }
        Integer remaining = quotaMapper.selectRemaining(order.getChildId());
        if (remaining == null || remaining <= 0) {
            throw new BusinessException(4010, "剩余可约次数不足");
        }
        // 每日限制：同一儿童一天只能预约一个时段
        if (quotaMapper.countActiveByChildAndDate(order.getChildId(), slot.getSlotDate()) > 0) {
            throw new BusinessException(4011, "同一儿童一天只能预约一个时段");
        }
        order.setOrderNo("R" + System.currentTimeMillis());
        order.setStatus(1);
        order.setStoreId(slot.getStoreId());
        order.setReserveDate(slot.getSlotDate());
        order.setReserveTimeStart(slot.getSlotStartTime());
        order.setReserveTimeEnd(slot.getSlotEndTime());
        order.setReserveType(order.getReserveType() == null ? 1 : order.getReserveType());
        order.setSource(order.getSource() == null ? 2 : order.getSource());
        if (order.getSource() == 2 && UserContext.get() != null) {
            order.setOperatorName(resolveCurrentUserRealName());
        }
        reserveOrderMapper.insert(order);
        // 满额原子占位（防超卖）；失败则整个事务回滚
        if (slotMapper.incrementBooked(slot.getId()) == 0) {
            throw new BusinessException(5002, "预约已满");
        }
        // 扣减次数 + 流水（与预约创建同事务）
        quotaMapper.changeRemaining(order.getChildId(), -1);
        quotaMapper.insertQuotaRecord(order.getChildId(), order.getStoreId(), 2, -1,
                order.getId(), UserContext.getCurrentUserId(), "预约扣减");
        return order.getId();
    }

    @Override
    @Transactional
    public void startCare(Long id, Map<String, String> params) {
        ReserveOrder order = reserveOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "预约不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(4012, "预约状态不允许开始养护");
        }
        if (!order.getReserveDate().equals(LocalDate.now())) {
            throw new BusinessException(4017, "仅预约当天可开始养护");
        }
        // 允许提前开始养护：客户可能早于预约时段到达，开始时间以弹窗选择的实际时间为准
        // 开始时间：优先前端选择的具体时间点，缺省当前时间
        LocalDateTime now = LocalDateTime.now();
        String startTimeStr = params == null ? null : params.get("startTime");
        if (startTimeStr != null && !startTimeStr.isBlank()) {
            try {
                LocalTime t = LocalTime.parse(startTimeStr.trim());
                order.setStartTime(LocalDateTime.of(order.getReserveDate(), t));
            } catch (java.time.format.DateTimeParseException ignored) {
                order.setStartTime(now);
            }
        } else {
            order.setStartTime(now);
        }
        // 执行人（养护服务医生）：优先前端选择，缺省当前登录人
        Long executorId = UserContext.getCurrentUserId();
        String executorName = resolveCurrentUserRealName();
        if (params != null && params.get("executorName") != null && !params.get("executorName").isBlank()) {
            executorName = params.get("executorName").trim();
            String executorIdStr = params.get("executorId");
            if (executorIdStr != null && !executorIdStr.isBlank()) {
                try {
                    executorId = Long.valueOf(executorIdStr);
                } catch (NumberFormatException ignored) {
                    // 保持当前用户ID
                }
            }
        }
        order.setStatus(2);
        order.setExecutorId(executorId);
        order.setExecutorName(executorName);
        reserveOrderMapper.updateById(order);

        // 同事务创建养护记录（录入养护前视力）
        CareRecord record = new CareRecord();
        record.setAppointmentId(order.getId());
        record.setChildId(order.getChildId());
        record.setStoreId(order.getStoreId());
        record.setCareDate(order.getReserveDate());
        record.setTimeSlot(order.getReserveTimeStart().format(HM) + "-" + order.getReserveTimeEnd().format(HM));
        record.setVisionBeforeLeft(params == null ? null : params.get("visionBeforeLeft"));
        record.setVisionBeforeRight(params == null ? null : params.get("visionBeforeRight"));
        record.setVisionBeforeBoth(params == null ? null : params.get("visionBeforeBoth"));
        record.setExecutorId(executorId);
        record.setExecutorName(executorName);
        record.setStatus(1);
        careRecordMapper.insert(record);
    }

    @Override
    public CareRecord getCareRecord(Long reserveId) {
        ReserveOrder order = reserveOrderMapper.selectById(reserveId);
        if (order == null) {
            throw new BusinessException(404, "预约不存在");
        }
        return careRecordMapper.selectByAppointmentId(reserveId);
    }

    /** 解析当前登录人真实姓名：医务人员按手机号查名册，其余查 sys_user，兜底用户名 */
    private String resolveCurrentUserRealName() {
        String username = UserContext.get() == null ? null : UserContext.get().getUsername();
        if (username == null) {
            return null;
        }
        String name = null;
        if (Integer.valueOf(6).equals(UserContext.getCurrentUserType())) {
            name = operatorMapper.selectStaffName(username);
        } else {
            name = operatorMapper.selectSysUserRealName(username);
        }
        return name != null && !name.isBlank() ? name : username;
    }

    @Override
    @Transactional
    public void completeCare(Long id, Map<String, String> params) {
        ReserveOrder order = reserveOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "预约不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 2) {
            throw new BusinessException(4014, "预约不在养护中状态");
        }
        order.setStatus(3);
        order.setCompletedAt(LocalDateTime.now());
        reserveOrderMapper.updateById(order);
        careRecordMapper.completeCare(order.getId(),
                params == null ? null : params.get("visionAfterLeft"),
                params == null ? null : params.get("visionAfterRight"),
                params == null ? null : params.get("visionAfterBoth"));
    }

    @Override
    @Transactional
    public void markNoShow(Long id) {
        ReserveOrder order = requireMarkableNoShow(id);
        applyNoShow(order, UserContext.getCurrentUserId(), null, "爽约（未到）", "爽约不退还");
    }

    @Override
    @Transactional
    public void autoMarkNoShow(Long id) {
        ReserveOrder order = requireMarkableNoShow(id);
        applyNoShow(order, null, "系统", "爽约（系统自动标记）", "爽约不退还（系统自动标记）");
    }

    private ReserveOrder requireMarkableNoShow(Long id) {
        ReserveOrder order = reserveOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "预约不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(4015, "仅已预约状态可标记爽约");
        }
        if (order.getReserveDate() != null && order.getReserveDate().isAfter(LocalDate.now())) {
            throw new BusinessException(4018, "仅预约当天及逾期的预约可标记爽约");
        }
        return order;
    }

    private void applyNoShow(ReserveOrder order, Long operatorId, String operatorName,
                             String reason, String noRefundRemark) {
        order.setStatus(4);
        order.setNoShowFlag(1);
        order.setRefundFlag(0);
        order.setCancelReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        if (StringUtils.hasText(operatorName)) {
            order.setCancelOperatorId(operatorId);
            order.setCancelOperatorName(operatorName);
        }
        reserveOrderMapper.updateById(order);

        if (order.getSlotId() != null) {
            slotMapper.decrementBooked(order.getSlotId());
        } else {
            scheduleMapper.decrementReserved(order.getScheduleId());
        }
        // 爽约不退还次数（与取消不同，手动/自动标记均不退还）
        quotaMapper.insertQuotaRecord(order.getChildId(), order.getStoreId(), 5, 0,
                order.getId(), operatorId, noRefundRemark);
    }

    @Override
    @Transactional
    public void adjustReserve(Long id, Long slotId) {
        ReserveOrder order = reserveOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "预约不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(4016, "仅已预约状态可调整");
        }
        if (order.getReserveDate() != null && order.getReserveTimeEnd() != null
                && LocalDateTime.of(order.getReserveDate(), order.getReserveTimeEnd()).isBefore(LocalDateTime.now())) {
            throw new BusinessException(4019, "预约时段已结束，不可调整，请使用标记爽约");
        }
        if (slotId == null) {
            throw new BusinessException(400, "请选择新的预约时段");
        }
        ScheduleSlot slot = slotMapper.selectById(slotId);
        if (slot == null || slot.getStatus() == null || slot.getStatus() != 1) {
            throw new BusinessException(4007, "预约时段不存在或已关闭");
        }
        if (LocalDateTime.of(slot.getSlotDate(), slot.getSlotStartTime()).isBefore(LocalDateTime.now())) {
            throw new BusinessException(4008, "只能调整到当前时间之后的时段");
        }
        if (!slot.getStoreId().equals(order.getStoreId())) {
            throw new BusinessException(400, "新时段与预约不属于同一家门店");
        }
        // 每日限制：换日期时目标日不能有该儿童的其他有效预约
        if (!slot.getSlotDate().equals(order.getReserveDate())
                && quotaMapper.countActiveByChildAndDate(order.getChildId(), slot.getSlotDate()) > 0) {
            throw new BusinessException(4011, "同一儿童一天只能预约一个时段");
        }

        // 释放旧时段名额
        if (order.getSlotId() != null) {
            slotMapper.decrementBooked(order.getSlotId());
        } else {
            scheduleMapper.decrementReserved(order.getScheduleId());
        }
        // 占用新时段（满额原子校验，失败整体回滚）
        if (slotMapper.incrementBooked(slotId) == 0) {
            throw new BusinessException(5002, "预约已满");
        }

        order.setReserveDate(slot.getSlotDate());
        order.setReserveTimeStart(slot.getSlotStartTime());
        order.setReserveTimeEnd(slot.getSlotEndTime());
        order.setSlotId(slotId);
        order.setScheduleId(null);
        // 记录调整标记与操作人（与取消操作人同一套姓名解析规则）
        order.setAdjustFlag(1);
        order.setAdjustOperatorId(UserContext.getCurrentUserId());
        String adjustUsername = UserContext.get() == null ? null : UserContext.get().getUsername();
        if (StringUtils.hasText(adjustUsername)) {
            String adjustName;
            if (Integer.valueOf(6).equals(UserContext.getCurrentUserType())) {
                adjustName = operatorMapper.selectStaffName(adjustUsername);
            } else {
                adjustName = operatorMapper.selectSysUserRealName(adjustUsername);
            }
            order.setAdjustOperatorName(StringUtils.hasText(adjustName) ? adjustName : adjustUsername);
        }
        reserveOrderMapper.updateById(order);
    }

    @Override
    public List<Map<String, Object>> statisticsReserves(Long storeId, LocalDate startDate, LocalDate endDate) {
        return reserveOrderMapper.statisticsByDateRange(storeId, startDate, endDate);
    }

    @Override
    public IPage<ReserveOrder> listReserves(Long storeId, Long childId, Integer status, List<Integer> statuses, Boolean noShowFlag, LocalDate date, LocalDate startDate, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<ReserveOrder> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(ReserveOrder::getStoreId, storeId);
        }
        if (childId != null) {
            wrapper.eq(ReserveOrder::getChildId, childId);
        }
        if (statuses != null && !statuses.isEmpty()) {
            applyStatusesFilter(wrapper, statuses);
        } else {
            if (status != null) {
                wrapper.eq(ReserveOrder::getStatus, status);
            }
            // 爽约与取消同用 status=4，靠 no_show_flag 区分：true 只看爽约，false 排除爽约
            if (Boolean.TRUE.equals(noShowFlag)) {
                wrapper.eq(ReserveOrder::getNoShowFlag, 1);
            } else if (Boolean.FALSE.equals(noShowFlag)) {
                wrapper.and(w -> w.isNull(ReserveOrder::getNoShowFlag).or().ne(ReserveOrder::getNoShowFlag, 1));
            }
        }
        if (date != null) {
            wrapper.eq(ReserveOrder::getReserveDate, date);
        } else if (startDate != null) {
            wrapper.ge(ReserveOrder::getReserveDate, startDate);
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> matchedChildIds = matchChildIds(keyword.trim());
            if (matchedChildIds.isEmpty()) {
                return new Page<>(page, size);
            }
            wrapper.in(ReserveOrder::getChildId, matchedChildIds);
        }
        wrapper.orderByAsc(ReserveOrder::getReserveDate)
                .orderByAsc(ReserveOrder::getReserveTimeStart)
                .orderByAsc(ReserveOrder::getId);
        IPage<ReserveOrder> p = reserveOrderMapper.selectPage(new Page<>(page, size), wrapper);

        // 从排班表补充 technicianName（storeName 由门店服务提供，此处暂不填充）
        for (ReserveOrder order : p.getRecords()) {
            Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
            if (schedule != null) {
                order.setTechnicianName(schedule.getTechnicianName());
            }
            fillChildInfo(order);
            fillOperatorNames(order);
        }
        return p;
    }

    /**
     * 多状态过滤：1-3 为真实 status；4=已取消、5=已爽约（同为 status=4，靠 no_show_flag 区分）。
     * 4 与 5 同时勾选等价于全部 status=4；只勾其一时按 no_show_flag 细分
     */
    private void applyStatusesFilter(LambdaQueryWrapper<ReserveOrder> wrapper, List<Integer> statuses) {
        Set<Integer> set = new HashSet<>(statuses);
        List<Integer> real = set.stream().filter(s -> s != null && s >= 1 && s <= 3).sorted().toList();
        boolean withCancel = set.contains(4);
        boolean withNoShow = set.contains(5);
        if (real.isEmpty() && !withCancel && !withNoShow) {
            return;
        }
        if (!withCancel && !withNoShow) {
            wrapper.in(ReserveOrder::getStatus, real);
            return;
        }
        if (withCancel && withNoShow) {
            if (real.isEmpty()) {
                wrapper.eq(ReserveOrder::getStatus, 4);
            } else {
                List<Integer> in = new ArrayList<>(real);
                in.add(4);
                wrapper.in(ReserveOrder::getStatus, in);
            }
            return;
        }
        if (real.isEmpty()) {
            wrapper.eq(ReserveOrder::getStatus, 4);
            appendNoShowCondition(wrapper, withNoShow);
            return;
        }
        wrapper.and(w -> {
            w.in(ReserveOrder::getStatus, real);
            w.or(x -> {
                x.eq(ReserveOrder::getStatus, 4);
                appendNoShowCondition(x, withNoShow);
            });
        });
    }

    /** withNoShow=true 取 status=4 且 no_show_flag=1；false 取 status=4 且非爽约 */
    private void appendNoShowCondition(LambdaQueryWrapper<ReserveOrder> w, boolean withNoShow) {
        if (withNoShow) {
            w.eq(ReserveOrder::getNoShowFlag, 1);
        } else {
            w.and(y -> y.isNull(ReserveOrder::getNoShowFlag).or().ne(ReserveOrder::getNoShowFlag, 1));
        }
    }

    /**
     * 操作人姓名归一化：历史数据将登录用户名（如 careld3）存进姓名列，读取时解析为真实姓名展示
     */
    private String normalizeOperatorName(String storedName) {
        if (!StringUtils.hasText(storedName)) {
            return storedName;
        }
        String name = operatorMapper.selectSysUserRealName(storedName);
        if (!StringUtils.hasText(name)) {
            name = operatorMapper.selectStaffName(storedName);
        }
        return StringUtils.hasText(name) ? name : storedName;
    }

    /**
     * 预约人/取消人/调整人姓名归一化；调整人历史缺名时按 adjust_operator_id → updated_by 兜底解析
     */
    private void fillOperatorNames(ReserveOrder order) {
        order.setOperatorName(normalizeOperatorName(order.getOperatorName()));
        order.setCancelOperatorName(normalizeOperatorName(order.getCancelOperatorName()));
        String adjustName = normalizeOperatorName(order.getAdjustOperatorName());
        if (!StringUtils.hasText(adjustName) && Integer.valueOf(1).equals(order.getAdjustFlag())) {
            Long operatorId = order.getAdjustOperatorId() != null ? order.getAdjustOperatorId() : order.getUpdatedBy();
            if (operatorId != null) {
                adjustName = operatorMapper.selectSysUserDisplayNameById(operatorId);
                if (!StringUtils.hasText(adjustName)) {
                    adjustName = operatorMapper.selectStaffNameById(operatorId);
                }
            }
        }
        order.setAdjustOperatorName(adjustName);
    }

    /**
     * 按儿童姓名/家长手机号模糊匹配档案（掩码优先，解密后内存比对），返回命中的儿童ID列表
     */
    private List<Long> matchChildIds(String keyword) {
        List<Long> ids = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> row : quotaMapper.selectChildSearchFields()) {
            Number idNum = (Number) row.get("id");
            Long childId = idNum == null ? null : idNum.longValue();
            if (childId == null) {
                continue;
            }
            String nameMask = (String) row.get("nameMask");
            String phoneMask = (String) row.get("phoneMask");
            if ((nameMask != null && nameMask.contains(keyword)) || (phoneMask != null && phoneMask.contains(keyword))) {
                ids.add(childId);
                continue;
            }
            String plainName = decryptForSearch((String) row.get("nameEncrypted"));
            String plainPhone = decryptForSearch((String) row.get("phoneEncrypted"));
            if ((StringUtils.hasText(plainName) && plainName.contains(keyword))
                    || (StringUtils.hasText(plainPhone) && plainPhone.contains(keyword))) {
                ids.add(childId);
            }
        }
        return ids;
    }

    /**
     * 真实密文用 AES 解密；兼容种子数据的 "ENC:明文" 占位格式；失败返回 null
     */
    private String decryptForSearch(String ciphertext) {
        if (!StringUtils.hasText(ciphertext)) {
            return null;
        }
        if (ciphertext.startsWith("ENC:")) {
            return ciphertext.substring(4);
        }
        try {
            return com.careld.common.security.AesUtil.decrypt(ciphertext, aesKey);
        } catch (Exception e) {
            return null;
        }
    }

    private void fillChildInfo(ReserveOrder order) {
        if (order == null || order.getChildId() == null) {
            return;
        }
        java.util.Map<String, String> mask = quotaMapper.selectChildMask(order.getChildId());
        // 姓名解密显示全名，解密失败回退掩码；手机号保持掩码展示
        String plainName = decryptForSearch(quotaMapper.selectChildNameEncrypted(order.getChildId()));
        order.setChildName(StringUtils.hasText(plainName) ? plainName
                : (mask == null ? null : mask.get("nameMask")));
        if (mask != null) {
            order.setParentPhone(mask.get("phoneMask"));
        }
    }

    @Override
    public ReserveOrder getReserveById(Long id) {
        ReserveOrder order = reserveOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "预约不存在");
        }
        Schedule schedule = scheduleMapper.selectById(order.getScheduleId());
        if (schedule != null) {
            order.setTechnicianName(schedule.getTechnicianName());
        }
        fillChildInfo(order);
        return order;
    }

    @Override
    @Transactional
    public void updateSchedule(Long id, Schedule schedule) {
        Schedule exist = scheduleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "排班不存在");
        }
        schedule.setId(id);
        scheduleMapper.updateById(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        Schedule exist = scheduleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "排班不存在");
        }
        if (exist.getReservedCount() != null && exist.getReservedCount() > 0) {
            throw new BusinessException(5003, "存在预约记录，不可删除");
        }
        scheduleMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchCreate(BatchScheduleRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null
                || request.getTimeSlots() == null || request.getTimeSlots().isEmpty()) {
            throw new BusinessException(400, "排班日期与时间段不能为空");
        }
        Set<Integer> weekDays = request.getWeekDays() == null ? null : new HashSet<>(request.getWeekDays());
        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            if (weekDays != null && !weekDays.isEmpty() && !weekDays.contains(date.getDayOfWeek().getValue())) {
                continue;
            }
            for (BatchScheduleRequest.TimeSlot slot : request.getTimeSlots()) {
                Schedule schedule = new Schedule();
                schedule.setStoreId(request.getStoreId());
                schedule.setScheduleDate(date);
                schedule.setTechnicianId(request.getTechnicianId());
                schedule.setTimeSlotStart(LocalTime.parse(slot.getStart(), hm));
                schedule.setTimeSlotEnd(LocalTime.parse(slot.getEnd(), hm));
                schedule.setMaxCapacity(slot.getCapacity());
                createSchedule(schedule);
            }
        }
    }
}
