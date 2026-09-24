package com.careld.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 医院短信费用账户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_notify_account")
@Schema(description = "医院通知费用账户")
public class StoreNotifyAccount extends BaseEntity {

    @Schema(description = "医院ID")
    private Long storeId;

    @Schema(description = "可用余额（元）")
    private BigDecimal balance;

    @Schema(description = "累计消费（元）")
    private BigDecimal totalFee;

    @Schema(description = "累计充值（元）")
    private BigDecimal totalRecharge;
}
