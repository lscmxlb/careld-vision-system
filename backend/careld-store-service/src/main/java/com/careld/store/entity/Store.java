package com.careld.store.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_info")
public class Store extends BaseEntity {
    private String storeCode;
    private Long agentId;
    private String storeName;
    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private String districtCode;
    private String districtName;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String contactName;
    private String contactPhone;
    private String businessHours;
    private Integer networkType;
    private Integer status;
    private java.time.LocalDate openTime;
    private java.time.LocalDate joinDate;
    private Integer bedCount;
    private Integer institutionType;
    private String remark;

    @TableField(exist = false)
    private String agentName;

    @TableField(exist = false)
    private String centerName;

    @TableField(exist = false)
    private String centerId;
}
