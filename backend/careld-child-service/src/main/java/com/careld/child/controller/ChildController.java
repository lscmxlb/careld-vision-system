package com.careld.child.controller;

import com.careld.child.dto.ChildRequest;
import com.careld.child.entity.ChildProfile;
import com.careld.child.service.ChildService;
import com.careld.common.result.Result;
import com.careld.common.security.DataScopeHelper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

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
                                           @RequestParam(value = "keyword", required = false) String keyword) {
        // 数据权限：门店用户注入 storeId
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        return Result.success(childService.listProfiles(effectiveStoreId, auditStatus, parentUserId, keyword));
    }

    @GetMapping("/pending-count")
    public Result<Long> pendingCount(@RequestParam(value = "storeId", required = false) Long storeId,
                                     @RequestParam(value = "parentUserId", required = false) Long parentUserId) {
        // 数据权限：门店用户注入 storeId
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        return Result.success(childService.countPending(effectiveStoreId, parentUserId));
    }

    @GetMapping("/{id}")
    public Result<ChildProfile> get(@PathVariable Long id) {
        return Result.success(childService.getProfile(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody ChildRequest request) {
        // 将前端字段映射到实体
        ChildProfile profile = new ChildProfile();
        profile.setNameMask(request.getName());        // 前端 name → 实体 nameMask
        profile.setPhoneMask(request.getPhone());      // 前端 phone → 实体 phoneMask
        profile.setBirthDate(request.getBirthDate());
        profile.setGender(request.getGender());
        profile.setEyeCondition(request.getEyeCondition());
        profile.setMedicalHistory(request.getMedicalHistory());
        profile.setAllergyInfo(request.getAllergyInfo());
        profile.setFamilyHistory(request.getFamilyHistory());
        // storeId: 前端未传则默认 1
        profile.setStoreId(request.getStoreId() != null ? request.getStoreId() : 1L);
        return Result.success(childService.createProfile(profile, aesKey));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ChildRequest request) {
        ChildProfile profile = new ChildProfile();
        profile.setNameMask(request.getName());
        profile.setPhoneMask(request.getPhone());
        profile.setBirthDate(request.getBirthDate());
        profile.setGender(request.getGender());
        profile.setEyeCondition(request.getEyeCondition());
        profile.setMedicalHistory(request.getMedicalHistory());
        profile.setAllergyInfo(request.getAllergyInfo());
        profile.setFamilyHistory(request.getFamilyHistory());
        childService.updateProfile(id, profile, aesKey);
        return Result.success();
    }

    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> params,
                              @RequestAttribute("userId") Long userId) {
        childService.auditProfile(id, (Integer) params.get("auditStatus"),
                                  (String) params.get("auditRemark"), userId);
        return Result.success();
    }

    @GetMapping("/search")
    public Result<List<ChildProfile>> search(@RequestParam("storeId") Long storeId, @RequestParam("keyword") String keyword) {
        return Result.success(childService.searchForTv(storeId, keyword));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        childService.deleteProfile(id);
        return Result.success();
    }
}
