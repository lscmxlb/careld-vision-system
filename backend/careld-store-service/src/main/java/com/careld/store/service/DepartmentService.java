package com.careld.store.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.store.entity.Department;

import java.util.List;

/**
 * 科室服务接口
 */
public interface DepartmentService {

    /**
     * 创建科室
     */
    Long createDepartment(Department department);

    /**
     * 更新科室
     */
    void updateDepartment(Long id, Department department);

    /**
     * 删除科室
     */
    void deleteDepartment(Long id);

    /**
     * 根据ID查询科室
     */
    Department getById(Long id);

    /**
     * 科室分页列表（storeId 为空时查全部）
     */
    IPage<Department> listDepartments(Long storeId, Integer page, Integer size);

    /**
     * 更新科室状态
     */
    void updateStatus(Long id, Integer status);
}
