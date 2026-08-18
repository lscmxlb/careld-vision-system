package com.careld.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("brand_hq")
public class BrandHq extends BaseEntity {
    private String brandName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private Integer status;
}
