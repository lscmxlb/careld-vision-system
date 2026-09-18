package com.careld.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 养护记录（表归属 vision-service 读取；本服务在「开始养护/完成养护」事务内写入）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("care_record")
public class CareRecord extends BaseEntity {
    private Long appointmentId;
    private Long childId;
    private Long storeId;
    private LocalDate careDate;
    /** 时段（如 09:30-10:30） */
    private String timeSlot;
    private String visionBeforeLeft;
    private String visionBeforeRight;
    private String visionBeforeBoth;
    private String visionAfterLeft;
    private String visionAfterRight;
    private String visionAfterBoth;
    private Long executorId;
    private String executorName;
    /** 1=养护中 2=已完成 */
    private Integer status;
}
