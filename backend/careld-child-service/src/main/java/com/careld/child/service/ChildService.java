package com.careld.child.service;
import com.careld.child.entity.ChildProfile;
import java.util.List;
public interface ChildService {
    Long createProfile(ChildProfile profile, String aesKey);
    void updateProfile(Long id, ChildProfile profile, String aesKey);
    void auditProfile(Long id, Integer auditStatus, String remark, Long auditorId);
    ChildProfile getProfile(Long id);
    List<ChildProfile> listProfiles(Long storeId, Integer auditStatus, Long parentUserId, String keyword);
    List<ChildProfile> searchForTv(Long storeId, String keyword);

    /** 待审核档案数量 */
    long countPending(Long storeId, Long parentUserId);

    /** 删除档案（逻辑删除） */
    void deleteProfile(Long id);
}
