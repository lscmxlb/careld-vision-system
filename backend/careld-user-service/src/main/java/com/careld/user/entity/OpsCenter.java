package com.careld.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ops_center")
public class OpsCenter extends BaseEntity {
    private String centerCode;
    private String centerName;
    private Long hqId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String region;
    private Integer status;

    @TableField(exist = false)
    private Integer agentCount;

    @TableField(exist = false)
    private Integer storeCount;
}
