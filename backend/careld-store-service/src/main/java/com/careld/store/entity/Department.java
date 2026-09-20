package com.careld.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 门店科室实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_department")
public class Department extends BaseEntity {

    /**
     * 所属门店ID(医院)
     */
    private Long storeId;

    /**
     * 科室编码
     */
    private String deptCode;

    /**
     * 科室名称
     */
    private String deptName;

    /**
     * 科室类型:1儿童保健科 2妇幼保健科 3中医科 4眼科 5其它科室
     */
    private Integer deptType;

    /**
     * 服务电话
     */
    private String servicePhone;

    /**
     * 收费标准(元)
     */
    private BigDecimal chargeStandard;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态:0禁用 1启用
     */
    private Integer status;
}
