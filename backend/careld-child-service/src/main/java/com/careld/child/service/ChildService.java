package com.careld.child.service;
import com.careld.child.entity.ChildProfile;
import com.careld.child.entity.ChildServiceRecord;
import java.util.List;
import java.util.Map;
public interface ChildService {
    Long createProfile(ChildProfile profile, String aesKey);
    void updateProfile(Long id, ChildProfile profile, String aesKey);
    /** 档案审核；auditStatus=1(通过) 时 doctorId 必填（指定主治医生） */
    void auditProfile(Long id, Integer auditStatus, String remark, Long auditorId, Long doctorId, String doctorName);
    /** 档案详情（返回明文姓名/手机号，列表接口仍返回掩码，家长不可见已隐藏档案） */
    ChildProfile getProfile(Long id, String aesKey);
    /** 档案列表；statuses 非空按状态集合过滤（医院端「全部」=0,1）；remainingCountMin 非空时仅返回可用次数大于该值的档案 */
    List<ChildProfile> listProfiles(Long storeId, Integer auditStatus, Long parentUserId, String keyword, boolean includeDisabled, Integer status, List<Integer> statuses, Integer remainingCountMin, String aesKey);
    List<ChildProfile> searchForTv(Long storeId, String keyword);

    /** 按监护人手机号精确核验本店儿童（密文解密比对，无密文老数据走掩码兜底） */
    List<ChildProfile> searchByGuardianPhone(Long storeId, String phone, String aesKey);

    /** 预约选人选项：仅已审核且启用的儿童，返回明文姓名/手机号与剩余次数，keyword 匹配明文姓名或手机号 */
    List<ChildProfile> pickOptions(Long storeId, String keyword, String aesKey);

    /** 待审核档案数量 */
    long countPending(Long storeId, Long parentUserId);

    /** 家长删除档案（语义为隐藏 status=2）；有未完成预约时拒绝，家长不可见、医生端/医院端可查看并恢复 */
    void hideProfile(Long id);

    /** 恢复已隐藏档案（status 2→1）；家长不可调用，门店用户限本店 */
    void restoreProfile(Long id);

    /** 家长按登录手机号认领未绑定档案（写 parent_user_id），返回 {claimedCount, parentName} */
    Map<String, Object> claimByPhone(String aesKey);

    /** 启用/禁用档案（status: 1=启用, 0=禁用） */
    void updateStatus(Long id, Integer status, Long operatorId);

    /** 添加服务记录（医生授予可约次数，需缴费金额/缴费方式/开单医生），返回记录ID */
    Long grantServiceRecord(Long childId, Integer count, java.math.BigDecimal paymentAmount,
                            String paymentMethod, Long doctorId, String doctorName,
                            String remark, Long operatorId);

    /** 服务次数变更流水 */
    List<ChildServiceRecord> listServiceRecords(Long childId);

    /**
     * 授权记录分页（含体验卡兑换）：change_type IN (1=预约授权, 6=体验卡兑换)，
     * changeType 传 1/6 时仅看对应类型；childName/phone 掩码与明文双模式匹配；doctorName 按开单医生姓名快照模糊；
     * startDate/endDate 为 yyyy-MM-dd。
     */
    com.careld.common.result.PageResult<ChildServiceRecord> pageAuthorizationRecords(
            Long storeId, String childName, String phone, Integer changeType, String doctorName,
            String startDate, String endDate, int page, int size, String aesKey);

    /** 授权记录开单医生姓名选项（去重快照，供筛选下拉；覆盖历史/非在职人员） */
    List<String> listAuthorizationDoctorNames(Long storeId);
}
