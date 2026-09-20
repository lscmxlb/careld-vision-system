package com.careld.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.store.entity.Department;
import com.careld.store.mapper.DepartmentMapper;
import com.careld.store.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

/**
 * 科室服务实现
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DepartmentMapper departmentMapper;

    @Override
    public Long createDepartment(Department department) {
        if (department.getDeptCode() == null || department.getDeptCode().isBlank()) {
            // 科室编码不由用户录入：新建时自动生成随机 6 位数字（唯一键 uk_store_dept 冲突则重试，
            // 覆盖软删除记录仍占用编码的情况）
            for (int i = 0; i < 20; i++) {
                department.setDeptCode(String.format("%06d", RANDOM.nextInt(1_000_000)));
                try {
                    departmentMapper.insert(department);
                    return department.getId();
                } catch (DuplicateKeyException ignored) {
                    // 编码重复，重新生成
                }
            }
            throw new BusinessException(500, "科室编码生成失败，请重试");
        }
        departmentMapper.insert(department);
        return department.getId();
    }

    @Override
    public void updateDepartment(Long id, Department department) {
        department.setId(id);
        // 科室编码不对外展示、由系统生成：空值不覆盖已有编码（避免保存后再次提交把编码清空）
        if (department.getDeptCode() == null || department.getDeptCode().isBlank()) {
            department.setDeptCode(null);
        }
        boolean clearCharge = department.getChargeStandard() == null;
        boolean clearPhone = department.getServicePhone() == null;
        if (clearCharge || clearPhone) {
            // 允许清空收费标准/服务电话：updateById 会跳过 null 字段，需显式将列置为 NULL
            LambdaUpdateWrapper<Department> wrapper = new LambdaUpdateWrapper<Department>()
                    .eq(Department::getId, id);
            if (clearCharge) {
                wrapper.set(Department::getChargeStandard, null);
            }
            if (clearPhone) {
                wrapper.set(Department::getServicePhone, null);
            }
            departmentMapper.update(department, wrapper);
        } else {
            departmentMapper.updateById(department);
        }
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            return;
        }
        Long remain = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                .eq(Department::getStoreId, department.getStoreId()));
        if (remain != null && remain <= 1) {
            throw new BusinessException(400, "默认科室长期保留，不可删除");
        }
        departmentMapper.deleteById(id);
    }

    @Override
    public Department getById(Long id) {
        return departmentMapper.selectById(id);
    }

    @Override
    public IPage<Department> listDepartments(Long storeId, Integer page, Integer size) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(Department::getStoreId, storeId);
        }
        wrapper.orderByAsc(Department::getSortOrder).orderByAsc(Department::getId);
        return departmentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Department department = new Department();
        department.setId(id);
        department.setStatus(status);
        departmentMapper.updateById(department);
    }
}
