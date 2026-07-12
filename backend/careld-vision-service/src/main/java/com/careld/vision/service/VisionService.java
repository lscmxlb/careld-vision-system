package com.careld.vision.service;
import com.careld.vision.entity.VisionTestRecord;
import java.util.List;
import java.util.Map;
public interface VisionService {
    Long createRecord(VisionTestRecord record);
    List<VisionTestRecord> listByChild(Long childId);
    Map<String, Object> compareVision(Long childId, Long reserveId);
    VisionTestRecord getRecord(Long id);
}
