package com.careld.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预约规则配置（每医院单行，默认值见建表语句）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appointment_config")
public class AppointmentConfig extends BaseEntity {
    private Long storeId;
    /** 家长可取消的提前小时数 */
    private Integer parentCancelHours;
    /** 医生可取消的提前小时数 */
    private Integer doctorCancelHours;
    /** 预约开始后超过该分钟数未开始视为可标记爽约 */
    private Integer noShowBufferMinutes;
    /** 开始养护后自动完成小时数 */
    private BigDecimal autoCompleteHours;
    /** 预约时段结束后超过该小时数仍未处理，自动标记爽约 */
    private Integer autoNoShowHours;
    /** 预约记录默认显示的状态，逗号分隔（1已预约2养护中3已完成4已取消5已爽约） */
    private String defaultShowStatuses;
}
