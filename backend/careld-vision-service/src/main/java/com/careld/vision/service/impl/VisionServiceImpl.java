package com.careld.vision.service.impl;

import com.careld.common.exception.BusinessException;
import com.careld.common.result.PageResult;
import com.careld.common.security.AesUtil;
import com.careld.common.security.DataScopeHelper;
import com.careld.vision.dto.VisionQuery;
import com.careld.vision.dto.VisionRecordGroup;
import com.careld.vision.entity.VisionTestRecord;
import com.careld.vision.mapper.VisionMapper;
import com.careld.vision.service.VisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisionServiceImpl implements VisionService {

    private static final int MAX_PAGE_SIZE = 1000;

    private final VisionMapper visionMapper;

    @Value("${encryption.key}")
    private String encryptionKey;

    @Override
    public Long createRecord(VisionTestRecord record) {
        record.setRecordCode("V" + System.currentTimeMillis());
        visionMapper.insert(record);
        return record.getId();
    }

    @Override
    public PageResult<VisionTestRecord> listRecords(VisionQuery query) {
        if (!prepare(query)) {
            return PageResult.of(List.of(), query.getPage(), query.getSize(), 0);
        }
        long total = visionMapper.countByQuery(query);
        if (total == 0) {
            return PageResult.of(List.of(), query.getPage(), query.getSize(), 0);
        }
        List<VisionTestRecord> records = visionMapper.selectPageByQuery(query);
        records.forEach(this::decryptChildName);
        return PageResult.of(records, query.getPage(), query.getSize(), total);
    }

    @Override
    public PageResult<VisionRecordGroup> listGroupedRecords(VisionQuery query) {
        if (!prepare(query)) {
            return PageResult.of(List.of(), query.getPage(), query.getSize(), 0);
        }
        long total = visionMapper.countGroupedByQuery(query);
        if (total == 0) {
            return PageResult.of(List.of(), query.getPage(), query.getSize(), 0);
        }
        List<VisionRecordGroup> rows = visionMapper.selectGroupedPage(query);
        rows.forEach(this::decryptChildName);
        fillImprovement(rows);
        return PageResult.of(rows, query.getPage(), query.getSize(), total);
    }

    @Override
    public Map<String, Object> compareVision(Long childId, Long reserveId) {
        List<VisionTestRecord> records = visionMapper.selectByChildAndReserve(childId, reserveId);
        Map<String, Object> result = new HashMap<>();
        result.put("childId", childId);
        result.put("reserveId", reserveId);
        VisionTestRecord beforeLeft = null, beforeRight = null, afterLeft = null, afterRight = null;
        for (VisionTestRecord r : records) {
            if (r.getTestType() == 1) {
                if (r.getEyeType() == 1) beforeLeft = r;
                else beforeRight = r;
            } else {
                if (r.getEyeType() == 1) afterLeft = r;
                else afterRight = r;
            }
        }
        // 用 HashMap 而非 Map.of：单侧缺失时允许 null，避免 NPE
        Map<String, Object> beforeTest = new HashMap<>();
        beforeTest.put("leftEye", beforeLeft != null ? beforeLeft.getVisionLevel() : null);
        beforeTest.put("rightEye", beforeRight != null ? beforeRight.getVisionLevel() : null);
        Map<String, Object> afterTest = new HashMap<>();
        afterTest.put("leftEye", afterLeft != null ? afterLeft.getVisionLevel() : null);
        afterTest.put("rightEye", afterRight != null ? afterRight.getVisionLevel() : null);
        result.put("beforeTest", beforeTest);
        result.put("afterTest", afterTest);
        return result;
    }

    @Override
    public VisionTestRecord getRecord(Long id) {
        VisionTestRecord record = visionMapper.selectDetailById(id);
        if (record == null) {
            throw new BusinessException(404, "视力检测记录不存在");
        }
        decryptChildName(record);
        return record;
    }

    /** 解析数据权限、姓名匹配与日期区间；返回 false 表示姓名筛选无命中，可直接返回空结果 */
    private boolean prepare(VisionQuery query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 20 : Math.min(query.getSize(), MAX_PAGE_SIZE);
        query.setPage(page);
        query.setSize(size);
        query.setOffset((page - 1) * size);
        query.setStartTime(parseDayStart(query.getStartDate()));
        query.setEndTime(parseDayEnd(query.getEndDate()));
        // 数据权限：门店用户强制本院，家长按所选医院（可为异地），总部/超管按入参
        query.setEffectiveStoreId(DataScopeHelper.resolveStoreIdWithParentChoice(query.getStoreId()));

        String childName = trimToNull(query.getChildName());
        if (childName != null) {
            List<Long> matched = resolveChildIdsByName(query.getEffectiveStoreId(), childName);
            if (matched.isEmpty()) {
                return false;
            }
            query.setChildIds(matched);
        }
        String keyword = trimToNull(query.getKeyword());
        if (keyword != null) {
            List<Long> matched = resolveChildIdsByName(query.getEffectiveStoreId(), keyword);
            query.setKeywordChildIds(matched.isEmpty() ? null : matched);
        }
        query.setChildName(childName);
        query.setKeyword(keyword);
        return true;
    }

    private LocalDateTime parseDayStart(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim()).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "开始日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    private LocalDateTime parseDayEnd(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim()).atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "结束日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    /** 改善情况：养护后相对同儿童最近一次养护前的差值（左右眼有值的取平均） */
    private void fillImprovement(List<VisionRecordGroup> rows) {
        List<Long> childIds = rows.stream()
                .filter(row -> row.getTestType() != null && row.getTestType() == 2 && row.getChildId() != null)
                .map(VisionRecordGroup::getChildId)
                .distinct()
                .toList();
        if (childIds.isEmpty()) {
            return;
        }
        Map<Long, List<VisionRecordGroup>> history = visionMapper.selectGroupedByChildIds(childIds).stream()
                .collect(Collectors.groupingBy(VisionRecordGroup::getChildId));
        for (VisionRecordGroup row : rows) {
            if (row.getTestType() == null || row.getTestType() != 2 || row.getTestTime() == null) {
                continue;
            }
            VisionRecordGroup baseline = history.getOrDefault(row.getChildId(), List.of()).stream()
                    .filter(item -> item.getTestType() != null && item.getTestType() == 1
                            && item.getTestTime() != null && !item.getTestTime().isAfter(row.getTestTime()))
                    .max(Comparator.comparing(VisionRecordGroup::getTestTime))
                    .orElse(null);
            if (baseline != null) {
                row.setImprovement(deltaText(baseline, row));
            }
        }
    }

    private String deltaText(VisionRecordGroup before, VisionRecordGroup after) {
        List<Double> deltas = new ArrayList<>();
        addDelta(deltas, before.getLeftEye(), after.getLeftEye());
        addDelta(deltas, before.getRightEye(), after.getRightEye());
        if (deltas.isEmpty()) {
            return null;
        }
        double average = deltas.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (Math.abs(average) < 0.05) {
            return "0.0";
        }
        return String.format("%+.1f", average);
    }

    private void addDelta(List<Double> deltas, String before, String after) {
        try {
            if (StringUtils.hasText(before) && StringUtils.hasText(after)) {
                deltas.add(Double.parseDouble(after.trim()) - Double.parseDouble(before.trim()));
            }
        } catch (NumberFormatException ignored) {
            // 视力值非标准数字时跳过该眼
        }
    }

    /** 姓名筛选：掩码与解密明文双模式模糊匹配（与儿童档案搜索口径一致，支持搜全名） */
    private List<Long> resolveChildIdsByName(Long storeId, String keyword) {
        return visionMapper.selectChildNameCandidates(storeId).stream()
                .filter(candidate -> matchesChildName(candidate, keyword))
                .map(VisionMapper.ChildNameCandidate::getId)
                .toList();
    }

    private boolean matchesChildName(VisionMapper.ChildNameCandidate candidate, String keyword) {
        if (candidate.getNameMask() != null && candidate.getNameMask().contains(keyword)) {
            return true;
        }
        String plainName = decryptForDetail(candidate.getNameEncrypted());
        return plainName != null && plainName.contains(keyword);
    }

    /** 列表/配对姓名：尝试解密为全名，失败保留掩码兜底；响应不下发密文 */
    private void decryptChildName(VisionTestRecord record) {
        String plainName = decryptForDetail(record.getChildNameEncrypted());
        if (StringUtils.hasText(plainName)) {
            record.setChildName(plainName);
        }
        record.setChildNameEncrypted(null);
    }

    private void decryptChildName(VisionRecordGroup row) {
        String plainName = decryptForDetail(row.getChildNameEncrypted());
        if (StringUtils.hasText(plainName)) {
            row.setChildName(plainName);
        }
        row.setChildNameEncrypted(null);
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
