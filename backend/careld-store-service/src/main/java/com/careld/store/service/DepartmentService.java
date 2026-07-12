package com.careld.store.service;

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
     * 根据门店ID查询科室列表
     */
    List<Department> listByStoreId(Long storeId);

    /**
     * 更新科室状态
     */
    void updateStatus(Long id, Integer status);
}
