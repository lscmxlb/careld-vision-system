package com.careld.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_type")
public class DeviceType extends BaseEntity {
    private String typeCode;
    private String typeName;
    private String description;
    private Integer defaultServiceLife;
    private Integer status;
}
