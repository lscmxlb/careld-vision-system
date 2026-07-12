package com.careld.sync.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sync_log")
public class SyncLog extends BaseEntity {
    private Long deviceId;
    private Long storeId;
    private Integer syncType;
    private String syncBatchId;
    private Integer recordCount;
    private Integer successCount;
    private Integer failCount;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMs;
    private String errorCode;
    private String errorMsg;
    private String requestData;
    private String responseData;
}
