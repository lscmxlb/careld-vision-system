package com.careld.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.store.entity.Department;
import com.careld.store.mapper.DepartmentMapper;
import com.careld.store.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 科室服务实现
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Override
    public Long createDepartment(Department department) {
        departmentMapper.insert(department);
        return department.getId();
    }

    @Override
    public void updateDepartment(Long id, Department department) {
        department.setId(id);
        departmentMapper.updateById(department);
    }

    @Override
    public void deleteDepartment(Long id) {
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
