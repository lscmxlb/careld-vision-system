package com.careld.sync.dto;
import lombok.Data;
import java.util.List;
import java.util.Map;
@Data
public class SyncUploadRequest {
    private String batchId;
    private Long deviceId;
    private Long storeId;
    private List<VisionRecord> records;
    @Data
    public static class VisionRecord {
        private String localId;
        private String childId;
        private String eyeType;
        private String visionLevel;
        private String testTime;
        private String beforeAfter;
        private String testerName;
        private String remark;
    }
}
