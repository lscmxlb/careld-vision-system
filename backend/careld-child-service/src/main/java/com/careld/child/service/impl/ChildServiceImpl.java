package com.careld.child.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.careld.child.entity.ChildProfile;
import com.careld.child.entity.ChildServiceRecord;
import com.careld.child.mapper.ChildMapper;
import com.careld.child.mapper.ChildServiceRecordMapper;
import com.careld.child.service.ChildService;
import com.careld.common.exception.BusinessException;
import com.careld.common.security.AesUtil;
import com.careld.common.security.DataScopeHelper;
import com.careld.common.security.UserContext;
import com.careld.common.utils.MaskUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChildServiceImpl implements ChildService {

    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of("自费支付", "医保-个人余额", "医保-统筹支付", "免费体验", "其它");

    private final ChildMapper childMapper;
    private final ChildServiceRecordMapper serviceRecordMapper;
    @Override
    @Transactional
    public Long createProfile(ChildProfile profile, String aesKey) {
        profile.setNameEncrypted(AesUtil.encrypt(profile.getNameMask(), aesKey));
        profile.setPhoneEncrypted(AesUtil.encrypt(profile.getPhoneMask(), aesKey));
        profile.setNameMask(MaskUtil.maskName(profile.getNameMask()));
        profile.setPhoneMask(MaskUtil.maskPhone(profile.getPhoneMask()));
        // 来源与审核：医生添加自动通过；家长添加待审核（来源由控制器按用户类型设置）
        if (profile.getSourceType() != null && profile.getSourceType() == 1) {
            profile.setAuditStatus(0);
        } else {
            profile.setSourceType(2);
            profile.setAuditStatus(1);
        }
        profile.setStatus(1);
        profile.setChildCode(generateChildCode());
        childMapper.insert(profile);
        return profile.getId();
    }
    @Override
    @Transactional
    public void updateProfile(Long id, ChildProfile profile, String aesKey) {
        ChildProfile exist = childMapper.selectById(id);
        if (exist == null) throw new BusinessException(404, "档案不存在");
        if (exist.getAuditStatus() == 0) throw new BusinessException(4001, "档案审核中，不可修改");
        // 姓名/手机号以明文传入，更新前须重新加密与脱敏（原实现把明文直接写进掩码列且密文永不更新，
        // 导致掩码列泄漏明文、监护人手机号变更后精确核验永远命中旧号）
        String name = profile.getNameMask();
        if (StringUtils.hasText(name)) {
            profile.setNameEncrypted(AesUtil.encrypt(name, aesKey));
            profile.setNameMask(MaskUtil.maskName(name));
        }
        String phone = profile.getPhoneMask();
        if (StringUtils.hasText(phone)) {
            profile.setPhoneEncrypted(AesUtil.encrypt(phone, aesKey));
            profile.setPhoneMask(MaskUtil.maskPhone(phone));
        }
        profile.setId(id);
        childMapper.updateById(profile);
    }
    @Override
    @Transactional
    public void auditProfile(Long id, Integer auditStatus, String remark, Long auditorId, Long doctorId, String doctorName) {
        if (auditStatus == null || (auditStatus != 0 && auditStatus != 1 && auditStatus != 2)) {
            throw new BusinessException(400, "审核状态非法（0=待审核 1=通过 2=驳回）");
        }
        if (auditStatus == 1 && doctorId == null) {
            throw new BusinessException(400, "审核通过前请指定主治医生");
        }
        ChildProfile profile = new ChildProfile();
        profile.setId(id);
        profile.setAuditStatus(auditStatus);
        profile.setAuditRemark(remark);
        profile.setAuditedBy(auditorId);
        profile.setAuditedAt(LocalDateTime.now());
        if (doctorId != null) {
            profile.setDoctorId(doctorId);
            profile.setDoctorName(doctorName);
        }
        childMapper.updateById(profile);
    }
    @Override
    public ChildProfile getProfile(Long id, String aesKey) {
        ChildProfile profile = childMapper.selectEnrichedById(id);
        if (profile == null) {
            throw new BusinessException(404, "档案不存在");
        }
        checkDetailScope(profile);
        enrich(profile);
        // 编辑场景需要明文：解密真实姓名/手机号，解不出时保留掩码兜底
        String plainName = decryptForDetail(profile.getNameEncrypted(), aesKey);
        if (StringUtils.hasText(plainName)) {
            profile.setName(plainName);
        }
        String plainPhone = decryptForDetail(profile.getPhoneEncrypted(), aesKey);
        if (StringUtils.hasText(plainPhone)) {
            profile.setPhone(plainPhone);
        }
        // 响应不下发密文列
        profile.setNameEncrypted(null);
        profile.setPhoneEncrypted(null);
        return profile;
    }

    /**
     * 详情解密：真实密文用 AES 解密；兼容种子数据的 "ENC:明文" 占位格式
     * （test-data.sql 无法在 SQL 内做 AES，密文列存的是占位串）。两者都不行返回 null，保留掩码。
     */
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

    /**
     * 详情接口返回明文 PII，按组织绑定链校验可见范围：
     * 门店用户仅本店、家长仅自己绑定的档案，总部/运营中心/代理商/超管放行
     */
    private void checkDetailScope(ChildProfile profile) {
        if (!DataScopeHelper.isRestricted()) {
            return;
        }
        Integer userType = UserContext.getCurrentUserType();
        if (userType != null && (userType == 4 || userType == 5)) {
            return;
        }
        if (userType != null && userType == 3) {
            Long parentUserId = UserContext.getCurrentUserId();
            if (parentUserId == null || !parentUserId.equals(profile.getParentUserId())) {
                throw new BusinessException(403, "无权查看该档案");
            }
            return;
        }
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null || !storeId.equals(profile.getStoreId())) {
            throw new BusinessException(403, "无权查看该档案");
        }
    }
    @Override
    public List<ChildProfile> listProfiles(Long storeId, Integer auditStatus, Long parentUserId, String keyword, boolean includeDisabled, Integer status, Integer remainingCountMin, String aesKey) {
        List<ChildProfile> list = childMapper.selectEnrichedList(storeId, auditStatus, parentUserId, null, includeDisabled, status);
        for (ChildProfile profile : list) {
            enrich(profile);
            // 列表姓名展示明文（解密失败回退掩码）；手机号仍脱敏
            String plainName = decryptForDetail(profile.getNameEncrypted(), aesKey);
            if (StringUtils.hasText(plainName)) {
                profile.setName(plainName);
            }
        }
        if (remainingCountMin != null) {
            list = list.stream()
                    .filter(p -> (p.getRemainingCount() == null ? 0 : p.getRemainingCount()) > remainingCountMin)
                    .toList();
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            list = list.stream().filter(p -> {
                String nameMask = p.getNameMask();
                String phoneMask = p.getPhoneMask();
                if (nameMask != null && nameMask.contains(kw)) {
                    return true;
                }
                if (phoneMask != null && phoneMask.contains(kw)) {
                    return true;
                }
                String plainName = decryptForDetail(p.getNameEncrypted(), aesKey);
                String plainPhone = decryptForDetail(p.getPhoneEncrypted(), aesKey);
                if (StringUtils.hasText(plainName) && plainName.contains(kw)) {
                    return true;
                }
                if (StringUtils.hasText(plainPhone) && plainPhone.contains(kw)) {
                    return true;
                }
                return false;
            }).toList();
        }
        return list;
    }
    @Override
    public List<ChildProfile> searchForTv(Long storeId, String keyword) {
        List<ChildProfile> list = childMapper.selectByStoreId(storeId);
        for (ChildProfile profile : list) {
            enrich(profile);
        }
        return list;
    }

    @Override
    public List<ChildProfile> searchByGuardianPhone(Long storeId, String phone, String aesKey) {
        List<ChildProfile> all = childMapper.selectEnrichedList(storeId, null, null, null, false, null);
        List<ChildProfile> matched = new ArrayList<>();
        String phoneMask = MaskUtil.maskPhone(phone);
        for (ChildProfile profile : all) {
            boolean hit = false;
            String encrypted = profile.getPhoneEncrypted();
            if (StringUtils.hasText(encrypted)) {
                try {
                    hit = phone.equals(AesUtil.decrypt(encrypted, aesKey));
                } catch (Exception ignored) {
                    // 密文损坏/密钥不匹配时退化为掩码比对
                }
            }
            if (!hit) {
                hit = phoneMask != null && phoneMask.equals(profile.getPhoneMask());
            }
            if (hit) {
                matched.add(profile);
            }
        }
        for (ChildProfile profile : matched) {
            enrich(profile);
            // 响应不下发密文列，姓名/手机号返回明文供预约选人展示
            String plainName = decryptForDetail(profile.getNameEncrypted(), aesKey);
            String plainPhone = decryptForDetail(profile.getPhoneEncrypted(), aesKey);
            profile.setNameEncrypted(null);
            profile.setPhoneEncrypted(null);
            profile.setName(plainName);
            profile.setPhone(StringUtils.hasText(plainPhone) ? plainPhone : phone);
        }
        return matched;
    }

    @Override
    public List<ChildProfile> pickOptions(Long storeId, String keyword, String aesKey) {
        List<ChildProfile> list = childMapper.selectEnrichedList(storeId, 1, null, null, false, 1);
        String kw = keyword == null ? "" : keyword.trim();
        List<ChildProfile> result = new ArrayList<>();
        for (ChildProfile profile : list) {
            String plainName = decryptForDetail(profile.getNameEncrypted(), aesKey);
            String plainPhone = decryptForDetail(profile.getPhoneEncrypted(), aesKey);
            if (!kw.isEmpty()) {
                boolean hit = (StringUtils.hasText(plainName) && plainName.contains(kw))
                        || (StringUtils.hasText(plainPhone) && plainPhone.contains(kw));
                if (!hit) {
                    continue;
                }
            }
            profile.setNameEncrypted(null);
            profile.setPhoneEncrypted(null);
            profile.setName(plainName);
            profile.setPhone(plainPhone);
            result.add(profile);
        }
        return result;
    }

    /**
     * 填充派生字段：name/phone 兼容字段 + lastVisionTest 嵌套对象
     */
    private void enrich(ChildProfile profile) {
        if (profile == null) {
            return;
        }
        profile.setName(profile.getNameMask());
        profile.setPhone(profile.getPhoneMask());
        if (profile.getLastLeftEye() != null || profile.getLastRightEye() != null || profile.getLastTestTime() != null) {
            profile.setLastVisionTest(new ChildProfile.LastVisionTest(
                    profile.getLastTestTime(), profile.getLastLeftEye(), profile.getLastRightEye()));
        }
    }
    @Override
    public long countPending(Long storeId, Long parentUserId) {
        return childMapper.countPending(storeId, parentUserId);
    }
    @Override
    @Transactional
    public void deleteProfile(Long id) {
        ChildProfile exist = childMapper.selectById(id);
        if (exist == null) throw new BusinessException(404, "档案不存在");
        childMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status, Long operatorId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值非法（仅允许 0=禁用 / 1=启用）");
        }
        ChildProfile exist = childMapper.selectById(id);
        if (exist == null) throw new BusinessException(404, "档案不存在");
        ChildProfile update = new ChildProfile();
        update.setId(id);
        update.setStatus(status);
        childMapper.updateById(update);
    }

    @Override
    @Transactional
    public Long grantServiceRecord(Long childId, Integer count, BigDecimal paymentAmount,
                                   String paymentMethod, Long doctorId, String doctorName,
                                   String remark, Long operatorId) {
        if (count == null || count <= 0) {
            throw new BusinessException(400, "授予次数必须大于 0");
        }
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "请填写缴费金额");
        }
        if (paymentMethod == null || !ALLOWED_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new BusinessException(400, "缴费方式仅允许：自费支付/医保-个人余额/医保-统筹支付/免费体验/其它");
        }
        if (doctorId == null) {
            throw new BusinessException(400, "请选择开单医生");
        }
        ChildProfile child = childMapper.selectById(childId);
        if (child == null) {
            throw new BusinessException(404, "档案不存在");
        }
        ChildServiceRecord record = new ChildServiceRecord();
        record.setChildId(childId);
        record.setStoreId(child.getStoreId());
        record.setChangeType(1);
        record.setChangeCount(count);
        record.setPaymentAmount(paymentAmount);
        record.setPaymentMethod(paymentMethod);
        record.setDoctorId(doctorId);
        record.setDoctorName(doctorName);
        record.setOperatorId(operatorId);
        record.setRemark(remark);
        serviceRecordMapper.insertRecord(record);
        serviceRecordMapper.changeRemaining(childId, count);
        return record.getId();
    }

    @Override
    public List<ChildServiceRecord> listServiceRecords(Long childId) {
        LambdaQueryWrapper<ChildServiceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChildServiceRecord::getChildId, childId);
        wrapper.orderByDesc(ChildServiceRecord::getCreatedAt)
                .orderByDesc(ChildServiceRecord::getId);
        List<ChildServiceRecord> records = serviceRecordMapper.selectList(wrapper);
        // 可用次数回溯：最新一条 = 当前剩余次数，更早一条 = 更新一条的可用次数 − 更新一条的变更次数
        ChildProfile child = childMapper.selectById(childId);
        int remaining = child == null || child.getRemainingCount() == null ? 0 : child.getRemainingCount();
        for (ChildServiceRecord record : records) {
            record.setRemainingAfter(remaining);
            remaining -= record.getChangeCount() == null ? 0 : record.getChangeCount();
        }
        return records;
    }
    private String generateChildCode() {
        return "CH" + System.currentTimeMillis();
    }
}
