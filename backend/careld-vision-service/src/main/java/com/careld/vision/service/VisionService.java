package com.careld.vision.service;

import com.careld.common.result.PageResult;
import com.careld.vision.dto.VisionQuery;
import com.careld.vision.dto.VisionRecordGroup;
import com.careld.vision.entity.VisionTestRecord;

import java.util.Map;

public interface VisionService {
    Long createRecord(VisionTestRecord record);
    PageResult<VisionTestRecord> listRecords(VisionQuery query);
    PageResult<VisionRecordGroup> listGroupedRecords(VisionQuery query);
    Map<String, Object> compareVision(Long childId, Long reserveId);
    VisionTestRecord getRecord(Long id);
}
