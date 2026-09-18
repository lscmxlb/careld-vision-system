package com.careld.child.controller;

import com.careld.child.dto.ChildRequest;
import com.careld.child.entity.ChildProfile;
import com.careld.child.entity.ChildServiceRecord;
import com.careld.child.service.ChildService;
import com.careld.common.exception.BusinessException;
import com.careld.common.result.Result;
import com.careld.common.security.DataScopeHelper;
import com.careld.common.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "儿童档案管理")
@RestController
@RequestMapping("/api/v1/children")
@RequiredArgsConstructor
public class ChildController {
    private final ChildService childService;

    @Value("${encryption.key:careld-vision-encrypt-key-32byte}")
    private String aesKey;

    @GetMapping
    public Result<List<ChildProfile>> list(@RequestParam(value = "storeId", required = false) Long storeId,
                                           @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
                                           @RequestParam(value = "parentUserId", required = false) Long parentUserId,
                                           @RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "includeDisabled", required = false) Boolean includeDisabled,
                                           @RequestParam(value = "remainingCountMin", required = false) Integer remainingCountMin,
                                           @RequestParam(value = "status", required = false) Integer status) {
        // 数据权限：家长按本人孩子过滤（门店不强制，可为异地医院），其他角色注入 storeId
        Long effectiveStoreId = DataScopeHelper.resolveStoreIdWithParentChoice(storeId);
        Long effectiveParentUserId = DataScopeHelper.resolveUserId(parentUserId);
        return Result.success(childService.listProfiles(effectiveStoreId, auditStatus, effectiveParentUserId, keyword, Boolean.TRUE.equals(includeDisabled), status, remainingCountMin, aesKey));
    }

    @GetMapping("/pending-count")
    public Result<Long> pendingCount(@RequestParam(value = "storeId", required = false) Long storeId,
                                     @RequestParam(value = "parentUserId", required = false) Long parentUserId) {
        // 数据权限：家长按本人孩子过滤（门店不强制），其他角色注入 storeId
        Long effectiveStoreId = DataScopeHelper.resolveStoreIdWithParentChoice(storeId);
        Long effectiveParentUserId = DataScopeHelper.resolveUserId(parentUserId);
        return Result.success(childService.countPending(effectiveStoreId, effectiveParentUserId));
    }

    @Operation(summary = "按监护人手机号精确核验本店儿童（门店预约选人用）")
    @GetMapping("/by-phone")
    public Result<List<ChildProfile>> searchByGuardianPhone(@RequestParam("phone") String phone,
                                                            @RequestParam(value = "storeId", required = false) Long storeId) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请输入正确的11位手机号");
        }
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        if (effectiveStoreId == null) {
            throw new BusinessException(400, "缺少门店信息，无法查询");
        }
        return Result.success(childService.searchByGuardianPhone(effectiveStoreId, phone, aesKey));
    }

    @Operation(summary = "预约选人选项（已审核且启用，明文姓名/手机号+剩余次数）")
    @GetMapping("/pick-options")
    public Result<List<ChildProfile>> pickOptions(@RequestParam(value = "storeId", required = false) Long storeId,
                                                  @RequestParam(value = "keyword", required = false) String keyword) {
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        if (effectiveStoreId == null) {
            throw new BusinessException(400, "缺少门店信息，无法查询");
        }
        return Result.success(childService.pickOptions(effectiveStoreId, keyword, aesKey));
    }

    @GetMapping("/{id}")
    public Result<ChildProfile> get(@PathVariable Long id) {
        return Result.success(childService.getProfile(id, aesKey));
    }

    @PostMapping
    public Result<Long> create(@RequestBody ChildRequest request) {
        // 将前端字段映射到实体
        ChildProfile profile = new ChildProfile();
        profile.setNameMask(request.getName());        // 前端 name → 实体 nameMask
        profile.setPhoneMask(request.getPhone());      // 前端 phone → 实体 phoneMask
        profile.setParentName(request.getParentName());
        profile.setRelation(request.getRelation());
        profile.setDoctorId(request.getDoctorId());
        profile.setDoctorName(request.getDoctorName());
        profile.setBirthDate(request.getBirthDate());
        profile.setGender(request.getGender());
        profile.setHomeAddress(request.getHomeAddress());
        profile.setSchool(request.getSchool());
        profile.setDeliveryType(request.getDeliveryType());
        profile.setBedtime(request.getBedtime());
        profile.setWakeTime(request.getWakeTime());
        profile.setEyeCondition(request.getEyeCondition());
        profile.setNakedVisionBoth(request.getNakedVisionBoth());
        profile.setNakedVisionLeft(request.getNakedVisionLeft());
        profile.setNakedVisionRight(request.getNakedVisionRight());
        profile.setMedicalHistory(request.getMedicalHistory());
        profile.setAllergyInfo(request.getAllergyInfo());
        profile.setFamilyHistory(request.getFamilyHistory());
        // storeId: 前端未传则归属当前用户所在门店（避免门店用户创建后因数据权限看不到）
        Long storeId = request.getStoreId();
        if (storeId == null) {
            storeId = UserContext.getCurrentStoreId() != null ? UserContext.getCurrentStoreId() : 1L;
        }
        profile.setStoreId(storeId);
        // 来源标记：家长（user_type=3）添加 → 待审核并绑定家长；医院侧添加 → 来源医生
        if (UserContext.getCurrentUserType() != null && UserContext.getCurrentUserType() == 3) {
            profile.setSourceType(1);
            profile.setParentUserId(UserContext.getCurrentUserId());
        } else {
            profile.setSourceType(2);
            profile.setSourceUserId(UserContext.getCurrentUserId());
        }
        return Result.success(childService.createProfile(profile, aesKey));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ChildRequest request) {
        ChildProfile profile = new ChildProfile();
        profile.setNameMask(request.getName());
        profile.setPhoneMask(request.getPhone());
        profile.setParentName(request.getParentName());
        profile.setRelation(request.getRelation());
        profile.setDoctorId(request.getDoctorId());
        profile.setDoctorName(request.getDoctorName());
        profile.setBirthDate(request.getBirthDate());
        profile.setGender(request.getGender());
        profile.setHomeAddress(request.getHomeAddress());
        profile.setSchool(request.getSchool());
        profile.setDeliveryType(request.getDeliveryType());
        profile.setBedtime(request.getBedtime());
        profile.setWakeTime(request.getWakeTime());
        profile.setEyeCondition(request.getEyeCondition());
        profile.setNakedVisionBoth(request.getNakedVisionBoth());
        profile.setNakedVisionLeft(request.getNakedVisionLeft());
        profile.setNakedVisionRight(request.getNakedVisionRight());
        profile.setMedicalHistory(request.getMedicalHistory());
        profile.setAllergyInfo(request.getAllergyInfo());
        profile.setFamilyHistory(request.getFamilyHistory());
        childService.updateProfile(id, profile, aesKey);
        return Result.success();
    }

    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> params,
                              @RequestAttribute("userId") Long userId) {
        Long doctorId = params.get("doctorId") == null ? null : ((Number) params.get("doctorId")).longValue();
        childService.auditProfile(id, (Integer) params.get("auditStatus"),
                (String) params.get("auditRemark"), userId, doctorId, (String) params.get("doctorName"));
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params,
                                     @RequestAttribute("userId") Long userId) {
        childService.updateStatus(id, (Integer) params.get("status"), userId);
        return Result.success();
    }

    @GetMapping("/search")
    public Result<List<ChildProfile>> search(@RequestParam("storeId") Long storeId, @RequestParam("keyword") String keyword) {
        return Result.success(childService.searchForTv(storeId, keyword));
    }

    @Operation(summary = "剩余可约次数")
    @GetMapping("/{id}/remaining-count")
    public Result<Integer> remainingCount(@PathVariable Long id) {
        ChildProfile profile = childService.getProfile(id, aesKey);
        return Result.success(profile.getRemainingCount() == null ? 0 : profile.getRemainingCount());
    }

    @Operation(summary = "添加服务记录（授予可约次数，需缴费金额/缴费方式/开单医生）")
    @PostMapping("/{id}/service-records")
    public Result<Long> addServiceRecord(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body,
                                         @RequestAttribute("userId") Long userId) {
        Integer count = body.get("changeCount") == null ? null : ((Number) body.get("changeCount")).intValue();
        BigDecimal paymentAmount = body.get("paymentAmount") == null ? null
                : new BigDecimal(body.get("paymentAmount").toString());
        String paymentMethod = (String) body.get("paymentMethod");
        Long doctorId = body.get("doctorId") == null ? null : ((Number) body.get("doctorId")).longValue();
        String doctorName = (String) body.get("doctorName");
        String remark = (String) body.get("remark");
        return Result.success(childService.grantServiceRecord(id, count, paymentAmount,
                paymentMethod, doctorId, doctorName, remark, userId));
    }

    @Operation(summary = "服务次数变更流水")
    @GetMapping("/{id}/service-records")
    public Result<List<ChildServiceRecord>> serviceRecords(@PathVariable Long id) {
        return Result.success(childService.listServiceRecords(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        childService.deleteProfile(id);
        return Result.success();
    }
}
