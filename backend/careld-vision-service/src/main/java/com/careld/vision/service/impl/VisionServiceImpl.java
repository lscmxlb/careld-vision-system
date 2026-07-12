package com.careld.vision.service.impl;
import com.careld.vision.entity.VisionTestRecord;
import com.careld.vision.mapper.VisionMapper;
import com.careld.vision.service.VisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
@Service
@RequiredArgsConstructor
public class VisionServiceImpl implements VisionService {
    private final VisionMapper visionMapper;
    @Override
    public Long createRecord(VisionTestRecord record) {
        record.setRecordCode("V" + System.currentTimeMillis());
        visionMapper.insert(record);
        return record.getId();
    }
    @Override
    public List<VisionTestRecord> listByChild(Long childId) {
        return visionMapper.selectByChildId(childId);
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
        result.put("beforeTest", Map.of("leftEye", beforeLeft != null ? beforeLeft.getVisionLevel() : null,
                                         "rightEye", beforeRight != null ? beforeRight.getVisionLevel() : null));
        result.put("afterTest", Map.of("leftEye", afterLeft != null ? afterLeft.getVisionLevel() : null,
                                        "rightEye", afterRight != null ? afterRight.getVisionLevel() : null));
        return result;
    }
    @Override
    public VisionTestRecord getRecord(Long id) { return visionMapper.selectById(id); }
}
