package com.careld.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
     * 科室类型:1门诊 2养护 3检测 4其他
     */
    private Integer deptType;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态:0禁用 1启用
     */
    private Integer status;
}
