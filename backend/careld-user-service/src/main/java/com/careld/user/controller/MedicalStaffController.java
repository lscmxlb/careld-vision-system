package com.careld.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.common.security.DataScopeHelper;
import com.careld.user.entity.MedicalStaff;
import com.careld.user.mapper.MedicalStaffMapper;
import com.careld.common.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 医务人员控制器（医院端自行管理医生/医生助理）
 */
@Tag(name = "医务人员管理", description = "医务人员CRUD操作")
@RestController
@RequestMapping("/api/v1/medical-staff")
@RequiredArgsConstructor
public class MedicalStaffController {

    private final MedicalStaffMapper medicalStaffMapper;
    private final PasswordEncoder passwordEncoder;

    /** 默认登录密码（新建留空与重置留空均使用） */
    private static final String DEFAULT_PASSWORD = "4009993608";

    @Operation(summary = "医务人员列表（总部用户可传storeId按医院筛选）")
    @GetMapping
    public Result<PageResult<MedicalStaff>> list(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "staffRole", required = false) Integer staffRole,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 数据权限：家长可为孩子选择任意医院建档，需按所选医院查医生；其他角色沿用原逻辑
        Long effectiveStoreId = DataScopeHelper.resolveStoreIdWithParentChoice(storeId);
        LambdaQueryWrapper<MedicalStaff> wrapper = new LambdaQueryWrapper<MedicalStaff>()
                .eq(MedicalStaff::getStoreId, effectiveStoreId)
                .eq(staffRole != null, MedicalStaff::getStaffRole, staffRole)
                .eq(status != null, MedicalStaff::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(MedicalStaff::getName, keyword)
                        .or().like(MedicalStaff::getPhone, keyword))
                .orderByDesc(MedicalStaff::getCreatedAt);
        Page<MedicalStaff> result = medicalStaffMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(s -> s.setLoginPassword(null));
        return Result.success(PageResult.of(result.getRecords(), page, size, result.getTotal()));
    }

    @OperationLog(module = "medical-staff", action = "create", description = "新增医务人员")
    @Operation(summary = "新增医务人员")
    @PostMapping
    public Result<MedicalStaff> create(@RequestBody MedicalStaff staff) {
        Long storeId = DataScopeHelper.resolveStoreId(staff.getStoreId());
        if (staff.getName() == null || staff.getName().isBlank()) {
            throw new BusinessException(400, "姓名不能为空");
        }
        if (staff.getPhone() == null || !staff.getPhone().matches("^1\\d{10}$")) {
            throw new BusinessException(400, "手机号格式不正确");
        }
        if (staff.getStaffRole() == null || (staff.getStaffRole() != 1 && staff.getStaffRole() != 2)) {
            throw new BusinessException(400, "角色必须为医生或医生助理");
        }
        if (medicalStaffMapper.selectByStoreAndPhone(storeId, staff.getPhone()) != null) {
            throw new BusinessException(400, "该手机号已存在于本院医务人员中");
        }
        staff.setId(null);
        staff.setStoreId(storeId);
        String rawPassword = (staff.getLoginPassword() == null || staff.getLoginPassword().isBlank())
                ? DEFAULT_PASSWORD : staff.getLoginPassword();
        staff.setLoginPassword(passwordEncoder.encode(rawPassword));
        if (staff.getStatus() == null) {
            staff.setStatus(1);
        }
        if (staff.getGender() == null) {
            staff.setGender(0);
        }
        medicalStaffMapper.insert(staff);
        staff.setLoginPassword(null);
        return Result.success(staff);
    }

    @OperationLog(module = "medical-staff", action = "update", description = "编辑医务人员")
    @Operation(summary = "修改医务人员")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MedicalStaff staff) {
        Long storeId = DataScopeHelper.resolveStoreId(null);
        MedicalStaff exist = medicalStaffMapper.selectById(id);
        // 总部用户（storeId=null）可管理任意医院的医务人员
        if (exist == null || (storeId != null && !storeId.equals(exist.getStoreId()))) {
            throw new BusinessException(404, "医务人员不存在");
        }
        // 手机号变更时校验同店唯一
        if (staff.getPhone() != null && !staff.getPhone().equals(exist.getPhone())) {
            if (!staff.getPhone().matches("^1\\d{10}$")) {
                throw new BusinessException(400, "手机号格式不正确");
            }
            MedicalStaff dup = medicalStaffMapper.selectByStoreAndPhone(storeId, staff.getPhone());
            if (dup != null && !dup.getId().equals(id)) {
                throw new BusinessException(400, "该手机号已存在于本院医务人员中");
            }
            exist.setPhone(staff.getPhone());
        }
        if (staff.getName() != null && !staff.getName().isBlank()) {
            exist.setName(staff.getName());
        }
        if (staff.getGender() != null) {
            exist.setGender(staff.getGender());
        }
        if (staff.getStaffRole() != null) {
            if (staff.getStaffRole() != 1 && staff.getStaffRole() != 2) {
                throw new BusinessException(400, "角色必须为医生或医生助理");
            }
            exist.setStaffRole(staff.getStaffRole());
        }
        if (staff.getStatus() != null) {
            exist.setStatus(staff.getStatus());
        }
        medicalStaffMapper.updateById(exist);
        return Result.success();
    }

    @OperationLog(module = "medical-staff", action = "status", description = "启用/禁用医务人员")
    @Operation(summary = "启用/禁用医务人员")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody MedicalStaff param) {
        Long storeId = DataScopeHelper.resolveStoreId(null);
        MedicalStaff exist = medicalStaffMapper.selectById(id);
        // 总部用户（storeId=null）可管理任意医院的医务人员
        if (exist == null || (storeId != null && !storeId.equals(exist.getStoreId()))) {
            throw new BusinessException(404, "医务人员不存在");
        }
        if (param.getStatus() == null || (param.getStatus() != 0 && param.getStatus() != 1)) {
            throw new BusinessException(400, "状态值不合法");
        }
        exist.setStatus(param.getStatus());
        medicalStaffMapper.updateById(exist);
        return Result.success();
    }

    @OperationLog(module = "medical-staff", action = "password", description = "重置医务人员密码")
    @Operation(summary = "重置登录密码")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody MedicalStaff param) {
        Long storeId = DataScopeHelper.resolveStoreId(null);
        MedicalStaff exist = medicalStaffMapper.selectById(id);
        // 总部用户（storeId=null）可管理任意医院的医务人员
        if (exist == null || (storeId != null && !storeId.equals(exist.getStoreId()))) {
            throw new BusinessException(404, "医务人员不存在");
        }
        String rawPassword = (param.getLoginPassword() == null || param.getLoginPassword().isBlank())
                ? DEFAULT_PASSWORD : param.getLoginPassword();
        exist.setLoginPassword(passwordEncoder.encode(rawPassword));
        medicalStaffMapper.updateById(exist);
        return Result.success();
    }

    @Operation(summary = "删除医务人员（录入后不可删除，仅可禁用）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long storeId = DataScopeHelper.resolveStoreId(null);
        MedicalStaff exist = medicalStaffMapper.selectById(id);
        // 总部用户（storeId=null）可管理任意医院的医务人员
        if (exist == null || (storeId != null && !storeId.equals(exist.getStoreId()))) {
            throw new BusinessException(404, "医务人员不存在");
        }
        throw new BusinessException(400, "医务人员不可删除，如需停用请使用「禁用」");
    }
}
