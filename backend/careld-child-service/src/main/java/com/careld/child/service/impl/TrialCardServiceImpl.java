package com.careld.child.service.impl;

import com.careld.child.entity.ChildProfile;
import com.careld.child.entity.ChildServiceRecord;
import com.careld.child.entity.TrialCard;
import com.careld.child.mapper.ChildMapper;
import com.careld.child.mapper.ChildServiceRecordMapper;
import com.careld.child.mapper.TrialCardMapper;
import com.careld.child.service.TrialCardService;
import com.careld.common.exception.BusinessException;
import com.careld.common.result.PageResult;
import com.careld.common.security.AesUtil;
import com.careld.common.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrialCardServiceImpl implements TrialCardService {

    /** 体验卡兑换产生的次数变更类型 */
    private static final int CHANGE_TYPE_TRIAL_REDEEM = 6;

    /** 单次发行数量上限 */
    private static final int MAX_ISSUE_COUNT = 10000;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TrialCardMapper trialCardMapper;
    private final ChildMapper childMapper;
    private final ChildServiceRecordMapper serviceRecordMapper;

    @Override
    public PageResult<TrialCard> listCards(Integer status, Long centerId, Long agentId, Long storeId,
                                           String keyword, String startDate, String endDate,
                                           int page, int size, String aesKey) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 500);
        long total = trialCardMapper.countEnriched(status, centerId, agentId, storeId, keyword, startDate, endDate);
        List<TrialCard> rows = total == 0
                ? List.of()
                : trialCardMapper.selectEnrichedPage(status, centerId, agentId, storeId, keyword, startDate, endDate,
                        (safePage - 1) * safeSize, safeSize);
        for (TrialCard row : rows) {
            row.setChildName(decryptForDetail(row.getChildNameEncrypted(), aesKey));
            row.setParentPhone(decryptForDetail(row.getParentPhoneEncrypted(), aesKey));
            row.setChildNameEncrypted(null);
            row.setParentPhoneEncrypted(null);
        }
        return PageResult.of(rows, safePage, safeSize, total);
    }

    /** 与儿童详情同口径：真实密文用 AES 解密，兼容种子数据 "ENC:明文" 占位 */
    private String decryptForDetail(String ciphertext, String aesKey) {
        if (!StringUtils.hasText(ciphertext)) {
            return null;
        }
        try {
            return AesUtil.decrypt(ciphertext, aesKey);
        } catch (Exception e) {
            if (ciphertext.startsWith("ENC:")) {
                return ciphertext.substring(4);
            }
            return null;
        }
    }

    @Override
    @Transactional
    public List<TrialCard> issueCards(Long centerId, Long agentId, Long storeId, String areaCodeRaw,
                                      String title, String remark, Integer count) {
        if (centerId == null) {
            throw new BusinessException(400, "请选择适用运营中心");
        }
        if (trialCardMapper.countCenter(centerId) == 0) {
            throw new BusinessException(400, "运营中心不存在");
        }
        if (agentId != null && trialCardMapper.countAgentInCenter(agentId, centerId) == 0) {
            throw new BusinessException(400, "所选代理商不属于该运营中心");
        }
        if (storeId != null) {
            Map<String, Object> org = childMapper.selectStoreOrg(storeId);
            Long storeCenterId = org == null ? null : longOf(org.get("centerId"));
            Long storeAgentId = org == null ? null : longOf(org.get("agentId"));
            if (storeCenterId == null) {
                throw new BusinessException(400, "该医院未绑定运营中心，不能作为适用范围");
            }
            if (!centerId.equals(storeCenterId)) {
                throw new BusinessException(400, "所选医院不属于该运营中心");
            }
            if (agentId != null && !agentId.equals(storeAgentId)) {
                throw new BusinessException(400, "所选医院不属于该代理商");
            }
            // 只选医院不选代理商时，按医院反推代理商一并落库，避免适用范围过宽
            if (agentId == null) {
                agentId = storeAgentId;
            }
        }
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty()) {
            throw new BusinessException(400, "请填写发行标题");
        }
        if (normalizedTitle.length() > 100) {
            throw new BusinessException(400, "发行标题最多 100 个字符");
        }
        int safeCount = count == null ? 1 : count;
        if (safeCount < 1 || safeCount > MAX_ISSUE_COUNT) {
            throw new BusinessException(400, "发行数量需在 1~" + MAX_ISSUE_COUNT + " 之间");
        }
        String areaCode = normalizeAreaCode(areaCodeRaw);
        String prefix = String.format("%02d", LocalDate.now().getYear() % 100) + areaCode;

        List<TrialCard> cards = new ArrayList<>(safeCount);
        // 顺序号预取：正常路径每张仅 1 次 INSERT，避免大批量发行时逐张 SELECT MAX
        int nextSeq = trialCardMapper.selectMaxSeq(prefix) + 1;
        for (int i = 0; i < safeCount; i++) {
            TrialCard card = insertOneCard(prefix, nextSeq, centerId, agentId, storeId, areaCode, normalizedTitle, remark);
            cards.add(card);
            nextSeq = card.getSeqNo() + 1;
        }
        return cards;
    }

    /** 单张落库：优先使用调用方预取的顺序号，撞号则重取前缀最大值重试（同批连续编号；全批失败即整体回滚） */
    private TrialCard insertOneCard(String prefix, int candidateSeq, Long centerId, Long agentId, Long storeId,
                                    String areaCode, String title, String remark) {
        TrialCard card = new TrialCard();
        card.setVerifyCode(generateVerifyCode());
        card.setTitle(title);
        card.setCenterId(centerId);
        card.setAgentId(agentId);
        card.setStoreId(storeId);
        card.setAreaCode(areaCode);
        card.setStatus(0);
        card.setRemark(remark);
        for (int attempt = 0; attempt < 3; attempt++) {
            int seq = attempt == 0 ? candidateSeq : trialCardMapper.selectMaxSeq(prefix) + 1;
            if (seq > 99999) {
                throw new BusinessException(400, "该区号本年度体验卡编号已用尽");
            }
            card.setSeqNo(seq);
            card.setCardNo(prefix + String.format("%05d", seq));
            try {
                trialCardMapper.insert(card);
                return card;
            } catch (DuplicateKeyException e) {
                // 并发发行撞号：重取顺序号再试
            }
        }
        throw new BusinessException(500, "体验卡编号生成失败，请重试");
    }

    /** 区号规范化：仅保留数字，不足 4 位前补 0（3 位区号如 755 → 0755） */
    private String normalizeAreaCode(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new BusinessException(400, "请填写运营中心所属城市电话区号");
        }
        if (digits.length() > 4) {
            throw new BusinessException(400, "区号最多 4 位数字");
        }
        return "0".repeat(4 - digits.length()) + digits;
    }

    /** 验证码 = 6 位随机数字 + 2 位校验位（前 6 位按位加权和 mod 100） */
    private String generateVerifyCode() {
        StringBuilder sb = new StringBuilder(8);
        int sum = 0;
        for (int i = 0; i < 6; i++) {
            int digit = RANDOM.nextInt(10);
            sb.append(digit);
            sum += digit * (i + 1);
        }
        return sb.append(String.format("%02d", sum % 100)).toString();
    }

    private boolean verifyCodeValid(String code) {
        if (code == null || !code.matches("\\d{8}")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 6; i++) {
            sum += (code.charAt(i) - '0') * (i + 1);
        }
        return sum % 100 == Integer.parseInt(code.substring(6));
    }

    @Override
    @Transactional
    public Map<String, Object> redeem(Long childId, String cardNo, String verifyCode) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (childId == null) {
            throw new BusinessException(400, "请选择要兑换的儿童档案");
        }
        if (cardNo == null || !cardNo.trim().matches("\\d{11}")) {
            throw new BusinessException(400, "请输入 11 位体验卡编号");
        }
        if (verifyCode == null || !verifyCode.trim().matches("\\d{8}")) {
            throw new BusinessException(400, "请输入 8 位体验卡验证码");
        }
        ChildProfile child = childMapper.selectById(childId);
        if (child == null) {
            throw new BusinessException(404, "档案不存在");
        }
        if (currentUserId == null || child.getParentUserId() == null
                || !child.getParentUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权操作该档案");
        }
        TrialCard card = trialCardMapper.selectByCardNo(cardNo.trim());
        if (card == null) {
            throw new BusinessException(400, "体验卡编号不存在，请核对后重试");
        }
        if (!verifyCodeValid(verifyCode.trim())) {
            throw new BusinessException(400, "体验卡验证码不正确");
        }
        if (card.getStatus() != null && card.getStatus() == 1) {
            throw new BusinessException(400, "该体验卡已于 " + formatTime(card.getUsedAt()) + " 使用，不能重复兑换");
        }
        if (card.getStatus() != null && card.getStatus() == 2) {
            throw new BusinessException(400, "该体验卡已于 " + formatTime(card.getBoundAt()) + " 绑定，不能重复兑换");
        }
        if (card.getStatus() != null && card.getStatus() == 3) {
            throw new BusinessException(400, "该体验卡已被禁用，不能兑换");
        }
        Map<String, Object> org = childMapper.selectStoreOrg(child.getStoreId());
        Long childCenterId = org == null ? null : longOf(org.get("centerId"));
        Long childAgentId = org == null ? null : longOf(org.get("agentId"));
        if (childCenterId == null || !childCenterId.equals(card.getCenterId())) {
            throw new BusinessException(400, "该体验卡不适用于当前档案所属医院");
        }
        if (card.getAgentId() != null && !card.getAgentId().equals(childAgentId)) {
            throw new BusinessException(400, "该体验卡不适用于当前档案所属医院");
        }
        if (card.getStoreId() != null && !card.getStoreId().equals(child.getStoreId())) {
            throw new BusinessException(400, "该体验卡不适用于当前档案所属医院");
        }
        int affected = trialCardMapper.markRedeemed(card.getId(), child.getStoreId(), child.getId(),
                currentUserId, currentUserId);
        if (affected == 0) {
            throw new BusinessException(400, "该体验卡已被兑换，不能重复兑换");
        }
        ChildServiceRecord record = new ChildServiceRecord();
        record.setChildId(child.getId());
        record.setStoreId(child.getStoreId());
        record.setChangeType(CHANGE_TYPE_TRIAL_REDEEM);
        record.setChangeCount(1);
        record.setPaymentAmount(BigDecimal.ZERO);
        record.setPaymentMethod("体验卡兑换");
        record.setOperatorId(currentUserId);
        record.setRemark("体验卡兑换（编号 " + card.getCardNo() + "）");
        serviceRecordMapper.insertRecord(record);
        serviceRecordMapper.changeRemaining(child.getId(), 1);

        int remaining = (child.getRemainingCount() == null ? 0 : child.getRemainingCount()) + 1;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("childId", child.getId());
        result.put("cardNo", card.getCardNo());
        result.put("changeCount", 1);
        result.put("remainingCount", remaining);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> disableCard(Long id) {
        TrialCard card = requireCard(id);
        if (card.getStatus() == null || card.getStatus() != 0) {
            throw new BusinessException(400, "仅未兑换的体验卡可以禁用");
        }
        if (trialCardMapper.markDisabled(id, UserContext.getCurrentUserId()) == 0) {
            throw new BusinessException(400, "体验卡状态已变化，请刷新后重试");
        }
        return statusResult(card, 3);
    }

    @Override
    @Transactional
    public Map<String, Object> enableCard(Long id) {
        TrialCard card = requireCard(id);
        if (card.getStatus() == null || card.getStatus() != 3) {
            throw new BusinessException(400, "仅已禁用的体验卡可以启用");
        }
        if (trialCardMapper.markEnabled(id, UserContext.getCurrentUserId()) == 0) {
            throw new BusinessException(400, "体验卡状态已变化，请刷新后重试");
        }
        return statusResult(card, 0);
    }

    @Override
    @Transactional
    public Map<String, Object> disableRange(String startCardNoRaw, String endCardNoRaw) {
        String startCardNo = startCardNoRaw == null ? "" : startCardNoRaw.trim();
        String endCardNo = endCardNoRaw == null ? "" : endCardNoRaw.trim();
        if (!startCardNo.matches("\\d{11}") || !endCardNo.matches("\\d{11}")) {
            throw new BusinessException(400, "请输入 11 位体验卡编号");
        }
        if (startCardNo.compareTo(endCardNo) > 0) {
            throw new BusinessException(400, "起始编号不能大于结束编号");
        }
        long total = trialCardMapper.countInRange(startCardNo, endCardNo);
        if (total == 0) {
            throw new BusinessException(400, "该编号区间内没有体验卡");
        }
        int disabled = trialCardMapper.disableRange(startCardNo, endCardNo, UserContext.getCurrentUserId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startCardNo", startCardNo);
        result.put("endCardNo", endCardNo);
        result.put("total", total);
        result.put("disabled", disabled);
        result.put("skipped", total - disabled);
        return result;
    }

    /** selectById 自带 @TableLogic 软删过滤，返回 null 即不存在 */
    private TrialCard requireCard(Long id) {
        TrialCard card = id == null ? null : trialCardMapper.selectById(id);
        if (card == null) {
            throw new BusinessException(404, "体验卡不存在");
        }
        return card;
    }

    private Map<String, Object> statusResult(TrialCard card, int status) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", card.getId());
        result.put("cardNo", card.getCardNo());
        result.put("status", status);
        return result;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "此前" : DATE_TIME.format(time);
    }

    private Long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
