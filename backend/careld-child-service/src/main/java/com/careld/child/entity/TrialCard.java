package com.careld.child.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 项目体验卡
 * status: 0=未兑换 2=已绑定（已兑换未核销） 1=已使用 3=已禁用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trial_card")
public class TrialCard extends BaseEntity {
    /** 编号（11位：2位年份+4位区号+5位顺序号） */
    private String cardNo;
    /** 验证码（8位数字，末2位为前6位的校验位） */
    private String verifyCode;
    /** 发行标题（本批体验卡的关键信息） */
    private String title;
    /** 适用运营中心（必填） */
    private Long centerId;
    /** 适用代理商（空=该运营中心下全部代理商） */
    private Long agentId;
    /** 适用医院（空=该运营中心/代理商下全部医院） */
    private Long storeId;
    /** 城市电话区号（4位，3位区号前补0） */
    private String areaCode;
    /** 同前缀（年份+区号）内的顺序编号 */
    private Integer seqNo;
    private Integer status;
    /** 绑定时间（家长兑换时写入，核销后仍保留） */
    private LocalDateTime boundAt;
    private LocalDateTime usedAt;
    private Long usedStoreId;
    private Long usedChildId;
    private Long usedParentUserId;
    private String remark;

    /* ---------- 以下为查询回填的展示字段（非落库） ---------- */
    @TableField(exist = false)
    private String centerName;
    @TableField(exist = false)
    private String agentName;
    /** 适用医院名（store_id 为空时为空） */
    @TableField(exist = false)
    private String storeName;
    /** 使用医院名 */
    @TableField(exist = false)
    private String usedStoreName;
    /** 使用的儿童姓名（解密后明文，仅后台列表/导出下发） */
    @TableField(exist = false)
    private String childName;
    /** 家长联系姓名 */
    @TableField(exist = false)
    private String parentName;
    /** 家长联系方式（解密后明文，仅后台列表/导出下发） */
    @TableField(exist = false)
    private String parentPhone;
    /** 发行人姓名（created_by 对应用户的真实姓名，空则取用户名；查询回填） */
    @TableField(exist = false)
    private String creatorName;
    @TableField(exist = false)
    private String childNameEncrypted;
    @TableField(exist = false)
    private String parentPhoneEncrypted;
}
