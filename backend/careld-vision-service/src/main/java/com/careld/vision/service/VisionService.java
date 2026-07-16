package com.careld.vision.service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.vision.entity.VisionTestRecord;
import java.util.List;
import java.util.Map;
public interface VisionService {
    Long createRecord(VisionTestRecord record);
    IPage<VisionTestRecord> listRecords(Long childId, Integer page, Integer size);
    Map<String, Object> compareVision(Long childId, Long reserveId);
    VisionTestRecord getRecord(Long id);
}
