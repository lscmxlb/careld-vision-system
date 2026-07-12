package com.careld.store.controller;

import com.careld.common.result.Result;
import com.careld.store.entity.Department;
import com.careld.store.service.DepartmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科室管理控制器
 */
@Tag(name = "科室管理")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public Result<List<Department>> list(@RequestParam("storeId") Long storeId) {
        return Result.success(departmentService.listByStoreId(storeId));
    }

    @GetMapping("/{id}")
    public Result<Department> get(@PathVariable Long id) {
        return Result.success(departmentService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody Department department) {
        return Result.success(departmentService.createDepartment(department));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Department department) {
        departmentService.updateDepartment(id, department);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody Department department) {
        departmentService.updateStatus(id, department.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return Result.success();
    }
}
