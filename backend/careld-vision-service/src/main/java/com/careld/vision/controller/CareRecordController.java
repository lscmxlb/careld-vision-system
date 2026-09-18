package com.careld.vision.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.common.security.AesUtil;
import com.careld.common.security.DataScopeHelper;
import com.careld.vision.entity.CareRecord;
import com.careld.vision.mapper.CareRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 养护记录查询（医院端横向展示 / 家长端瀑布流，共用同一数据）
 */
@Tag(name = "养护记录")
@RestController
@RequestMapping("/api/v1/care-records")
@RequiredArgsConstructor
public class CareRecordController {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final CareRecordMapper careRecordMapper;

    @Value("${encryption.key}")
    private String encryptionKey;

    @Operation(summary = "养护记录列表（按儿童查询，时间倒序）")
    @GetMapping
    public Result<List<CareRecord>> list(@RequestParam("childId") Long childId,
                                         @RequestParam(value = "storeId", required = false) Long storeId) {
        LambdaQueryWrapper<CareRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CareRecord::getChildId, childId);
        // 数据权限：家长按所选医院（可为异地），其他角色沿用原逻辑
        Long effectiveStoreId = DataScopeHelper.resolveStoreIdWithParentChoice(storeId);
        if (effectiveStoreId != null) {
            wrapper.eq(CareRecord::getStoreId, effectiveStoreId);
        }
        wrapper.orderByDesc(CareRecord::getCareDate);
        wrapper.orderByDesc(CareRecord::getId);
        List<CareRecord> records = careRecordMapper.selectList(wrapper);
        fillActualCarePeriod(records);
        return Result.success(records);
    }

    @Operation(summary = "养护记录分页（按医院查询，支持儿童姓名/家长姓名/手机号/养护次数筛选）")
    @GetMapping("/page")
    public Result<PageResult<CareRecord>> page(@RequestParam(value = "storeId", required = false) Long storeId,
                                               @RequestParam(value = "childId", required = false) Long childId,
                                               @RequestParam(value = "childName", required = false) String childName,
                                               @RequestParam(value = "parentName", required = false) String parentName,
                                               @RequestParam(value = "phone", required = false) String phone,
                                               @RequestParam(value = "minCareCount", required = false) Integer minCareCount,
                                               @RequestParam(value = "page", defaultValue = "1") Integer page,
                                               @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 家长数据以本人孩子为准：不带具体孩子时不返回任何记录，避免跨院串看
        if (DataScopeHelper.isParent() && childId == null) {
            return Result.success(PageResult.of(List.of(), page, size, 0));
        }
        // 数据权限：家长按所选医院（可为异地），其他角色沿用原逻辑
        Long effectiveStoreId = DataScopeHelper.resolveStoreIdWithParentChoice(storeId);
        List<Long> matchedChildIds = null;
        if (StringUtils.hasText(childName)) {
            matchedChildIds = resolveChildIdsByName(effectiveStoreId, childName.trim());
            if (matchedChildIds.isEmpty()) {
                return Result.success(PageResult.of(List.of(), page, size, 0));
            }
        }
        IPage<CareRecord> p = careRecordMapper.selectPageWithChild(new Page<>(page, size),
                effectiveStoreId, childId, matchedChildIds, parentName, phone, minCareCount);
        p.getRecords().forEach(this::decryptChildName);
        fillActualCarePeriod(p.getRecords());
        return Result.success(PageResult.of(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal()));
    }

    @Operation(summary = "养护记录详情")
    @GetMapping("/{id}")
    public Result<CareRecord> get(@PathVariable Long id) {
        CareRecord record = careRecordMapper.selectDetailById(id);
        if (record == null) {
            throw new BusinessException(404, "养护记录不存在");
        }
        decryptChildName(record);
        fillActualCarePeriod(List.of(record));
        return Result.success(record);
    }

    /**
     * 养护时段以医师/医助实际录入的开始、结束养护时间为准：
     * 已完成取实际结束时间；未录入结束时间（养护中或无有效结束）按开始时间+60分钟；无实际开始时间保留原预约时段。
     */
    private void fillActualCarePeriod(List<CareRecord> records) {
        List<Long> appointmentIds = records.stream()
                .map(CareRecord::getAppointmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (appointmentIds.isEmpty()) {
            return;
        }
        Map<Long, CareRecordMapper.CarePeriodSource> sources = careRecordMapper.selectCarePeriodSources(appointmentIds)
                .stream()
                .collect(Collectors.toMap(CareRecordMapper.CarePeriodSource::getAppointmentId, Function.identity()));
        for (CareRecord record : records) {
            CareRecordMapper.CarePeriodSource source = sources.get(record.getAppointmentId());
            if (source == null || source.getStartTime() == null) {
                continue;
            }
            LocalDateTime end = source.getCompletedAt();
            if (end == null || !end.isAfter(source.getStartTime())) {
                end = source.getStartTime().plusMinutes(60);
            }
            record.setTimeSlot(source.getStartTime().format(HM) + "-" + end.format(HM));
        }
    }

    /** 列表姓名：尝试解密为全名，失败保留掩码兜底；响应不下发密文 */
    private void decryptChildName(CareRecord record) {
        String plainName = decryptForDetail(record.getChildNameEncrypted());
        if (StringUtils.hasText(plainName)) {
            record.setChildName(plainName);
        }
        record.setChildNameEncrypted(null);
    }

    /** 姓名筛选：掩码与解密明文双模式模糊匹配（与儿童档案搜索口径一致，支持搜全名） */
    private List<Long> resolveChildIdsByName(Long storeId, String keyword) {
        return careRecordMapper.selectChildNameCandidates(storeId).stream()
                .filter(candidate -> matchesChildName(candidate, keyword))
                .map(CareRecordMapper.ChildNameCandidate::getId)
                .toList();
    }

    private boolean matchesChildName(CareRecordMapper.ChildNameCandidate candidate, String keyword) {
        if (candidate.getNameMask() != null && candidate.getNameMask().contains(keyword)) {
            return true;
        }
        String plainName = decryptForDetail(candidate.getNameEncrypted());
        return plainName != null && plainName.contains(keyword);
    }

    /**
     * 详情解密：真实密文用 AES 解密；兼容种子数据的 "ENC:明文" 占位格式。
     * 两者都不行返回 null，保留掩码。
     */
    private String decryptForDetail(String ciphertext) {
        if (!StringUtils.hasText(ciphertext)) {
            return null;
        }
        try {
            return AesUtil.decrypt(ciphertext, encryptionKey);
        } catch (Exception e) {
            if (ciphertext.startsWith("ENC:")) {
                return ciphertext.substring(4);
            }
            return null;
        }
    }
}
