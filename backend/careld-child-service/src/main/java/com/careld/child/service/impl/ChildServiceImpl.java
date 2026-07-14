package com.careld.child.service.impl;
import com.careld.child.entity.ChildProfile;
import com.careld.child.mapper.ChildMapper;
import com.careld.child.service.ChildService;
import com.careld.common.exception.BusinessException;
import com.careld.common.security.AesUtil;
import com.careld.common.utils.MaskUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class ChildServiceImpl implements ChildService {
    private final ChildMapper childMapper;
    @Override
    @Transactional
    public Long createProfile(ChildProfile profile, String aesKey) {
        profile.setNameEncrypted(AesUtil.encrypt(profile.getNameMask(), aesKey));
        profile.setPhoneEncrypted(AesUtil.encrypt(profile.getPhoneMask(), aesKey));
        profile.setNameMask(MaskUtil.maskName(profile.getNameMask()));
        profile.setPhoneMask(MaskUtil.maskPhone(profile.getPhoneMask()));
        profile.setAuditStatus(0);
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
        profile.setId(id);
        childMapper.updateById(profile);
    }
    @Override
    @Transactional
    public void auditProfile(Long id, Integer auditStatus, String remark, Long auditorId) {
        ChildProfile profile = new ChildProfile();
        profile.setId(id);
        profile.setAuditStatus(auditStatus);
        profile.setAuditRemark(remark);
        profile.setAuditedBy(auditorId);
        profile.setAuditedAt(LocalDateTime.now());
        childMapper.updateById(profile);
    }
    @Override
    public ChildProfile getProfile(Long id) {
        ChildProfile profile = childMapper.selectEnrichedById(id);
        if (profile == null) {
            throw new BusinessException(404, "档案不存在");
        }
        enrich(profile);
        return profile;
    }
    @Override
    public List<ChildProfile> listProfiles(Long storeId, Integer auditStatus, Long parentUserId, String keyword) {
        List<ChildProfile> list = childMapper.selectEnrichedList(storeId, auditStatus, parentUserId, keyword);
        for (ChildProfile profile : list) {
            enrich(profile);
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
    private String generateChildCode() {
        return "CH" + System.currentTimeMillis();
    }
}
