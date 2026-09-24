package com.careld.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.notify.entity.StoreNotifyAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface StoreNotifyAccountMapper extends BaseMapper<StoreNotifyAccount> {

    /**
     * 扣费（余额不足时不更新，返回 0，由调用方判定为「余额不足」）
     */
    @Update("UPDATE store_notify_account SET balance = balance - #{fee}, total_fee = total_fee + #{fee}, "
            + "updated_at = NOW() WHERE store_id = #{storeId} AND balance >= #{fee} AND deleted_at IS NULL")
    int deductFee(@Param("storeId") Long storeId, @Param("fee") BigDecimal fee);

    /** 发送失败退回预扣费用（余额加回、累计消费冲减） */
    @Update("UPDATE store_notify_account SET balance = balance + #{fee}, total_fee = total_fee - #{fee}, "
            + "updated_at = NOW() WHERE store_id = #{storeId} AND deleted_at IS NULL")
    int refundFee(@Param("storeId") Long storeId, @Param("fee") BigDecimal fee);

    /** 充值（模拟支付到账） */
    @Update("UPDATE store_notify_account SET balance = balance + #{amount}, total_recharge = total_recharge + #{amount}, "
            + "updated_at = NOW() WHERE store_id = #{storeId} AND deleted_at IS NULL")
    int recharge(@Param("storeId") Long storeId, @Param("amount") BigDecimal amount);
}
