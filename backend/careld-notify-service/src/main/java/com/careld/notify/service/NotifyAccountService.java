package com.careld.notify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.careld.common.exception.BusinessException;
import com.careld.notify.entity.StoreNotifyAccount;
import com.careld.notify.mapper.StoreNotifyAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 医院短信费用账户（余额 / 累计消费 / 累计充值）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyAccountService {

    private final StoreNotifyAccountMapper accountMapper;

    /** 取账户，缺失时初始化为 0 余额 */
    @Transactional
    public StoreNotifyAccount getOrCreate(Long storeId) {
        StoreNotifyAccount exist = select(storeId);
        if (exist != null) {
            return exist;
        }
        StoreNotifyAccount account = new StoreNotifyAccount();
        account.setStoreId(storeId);
        account.setBalance(BigDecimal.ZERO);
        account.setTotalFee(BigDecimal.ZERO);
        account.setTotalRecharge(BigDecimal.ZERO);
        try {
            accountMapper.insert(account);
            return account;
        } catch (Exception e) {
            StoreNotifyAccount created = select(storeId);
            if (created != null) {
                return created;
            }
            throw e;
        }
    }

    /**
     * 发送前预扣费用：余额不足时不扣并返回 false（调用方按「停发」处理）
     */
    public boolean tryDeduct(Long storeId, BigDecimal fee) {
        if (fee == null || fee.signum() <= 0) {
            return true;
        }
        getOrCreate(storeId);
        return accountMapper.deductFee(storeId, fee) > 0;
    }

    /** 发送失败退回预扣费用 */
    public void refund(Long storeId, BigDecimal fee) {
        if (fee == null || fee.signum() <= 0) {
            return;
        }
        accountMapper.refundFee(storeId, fee);
    }

    /** 充值到账（由充值订单支付成功后调用；仅累加余额与累计充值） */
    @Transactional
    public StoreNotifyAccount recharge(Long storeId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(400, "请输入正确的充值金额");
        }
        StoreNotifyAccount account = getOrCreate(storeId);
        accountMapper.recharge(storeId, amount);
        return select(storeId);
    }

    private StoreNotifyAccount select(Long storeId) {
        return accountMapper.selectOne(new LambdaQueryWrapper<StoreNotifyAccount>()
                .eq(StoreNotifyAccount::getStoreId, storeId)
                .orderByAsc(StoreNotifyAccount::getId)
                .last("LIMIT 1"));
    }
}
