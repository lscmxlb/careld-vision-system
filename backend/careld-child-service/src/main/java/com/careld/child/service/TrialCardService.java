package com.careld.child.service;

import com.careld.child.entity.TrialCard;
import com.careld.common.result.PageResult;

import java.util.List;
import java.util.Map;

public interface TrialCardService {
    /** 体验卡列表（后台医院管理；儿童姓名/家长联系方式解密下发） */
    PageResult<TrialCard> listCards(Integer status, Long centerId, Long agentId, Long storeId,
                                    String keyword, String startDate, String endDate,
                                    int page, int size, String aesKey);

    /** 批量发行体验卡（编号=2位年份+4位区号+5位顺序号，验证码=6位随机+2位校验位；同批连续编号、共用一个标题） */
    List<TrialCard> issueCards(Long centerId, Long agentId, Long storeId, String areaCode,
                               String title, String remark, Integer count);

    /** 家长兑换：校验归属/验证码/适用范围后次数+1，返回 {childId, remainingCount, cardNo} */
    Map<String, Object> redeem(Long childId, String cardNo, String verifyCode);

    /** 禁用体验卡：仅未兑换（0→3），返回 {id, cardNo, status} */
    Map<String, Object> disableCard(Long id);

    /** 启用体验卡：仅已禁用（3→0），返回 {id, cardNo, status} */
    Map<String, Object> enableCard(Long id);

    /** 按编号区间批量禁用：仅命中未兑换，返回 {total, disabled, skipped}（skipped=已绑定/已使用/已禁用） */
    Map<String, Object> disableRange(String startCardNo, String endCardNo);
}
