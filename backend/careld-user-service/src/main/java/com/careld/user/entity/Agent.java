package com.careld.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent")
public class Agent extends BaseEntity {
    private String agentCode;
    private String agentName;
    private Long centerId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String region;
    private Integer status;

    @TableField(exist = false)
    private String centerName;

    @TableField(exist = false)
    private Integer storeCount;
}
