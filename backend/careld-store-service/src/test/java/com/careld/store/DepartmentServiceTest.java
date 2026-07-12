package com.careld.store;

import com.careld.store.entity.Department;
import com.careld.store.mapper.DepartmentMapper;
import com.careld.store.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 科室服务测试
 */
@SpringBootTest
public class DepartmentServiceTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Test
    public void testCreateDepartment() {
        Department department = new Department();
        department.setStoreId(1L);
        department.setDeptCode("DEPT_001");
        department.setDeptName("养护科");
        department.setDeptType(2);
        department.setSortOrder(1);
        department.setStatus(1);

        Long id = departmentService.createDepartment(department);
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    public void testGetDepartment() {
        Department department = new Department();
        department.setStoreId(1L);
        department.setDeptCode("DEPT_002");
        department.setDeptName("检测科");
        department.setDeptType(3);
        department.setSortOrder(2);
        department.setStatus(1);

        Long id = departmentService.createDepartment(department);
        Department found = departmentService.getById(id);

        assertNotNull(found);
        assertEquals("检测科", found.getDeptName());
        assertEquals(3, found.getDeptType());
    }

    @Test
    public void testUpdateDepartment() {
        Department department = new Department();
        department.setStoreId(1L);
        department.setDeptCode("DEPT_003");
        department.setDeptName("门诊科");
        department.setDeptType(1);
        department.setSortOrder(3);
        department.setStatus(1);

        Long id = departmentService.createDepartment(department);

        Department update = new Department();
        update.setDeptName("门诊科（更新）");
        update.setSortOrder(4);

        departmentService.updateDepartment(id, update);
        Department found = departmentService.getById(id);

        assertEquals("门诊科（更新）", found.getDeptName());
        assertEquals(4, found.getSortOrder());
    }

    @Test
    public void testDeleteDepartment() {
        Department department = new Department();
        department.setStoreId(1L);
        department.setDeptCode("DEPT_004");
        department.setDeptName("其他科");
        department.setDeptType(4);
        department.setSortOrder(5);
        department.setStatus(1);

        Long id = departmentService.createDepartment(department);
        departmentService.deleteDepartment(id);

        Department found = departmentService.getById(id);
        assertNull(found);
    }

    @Test
    public void testListByStoreId() {
        // 清理测试数据
        // departmentMapper.delete(null);

        Department dept1 = new Department();
        dept1.setStoreId(1L);
        dept1.setDeptCode("DEPT_005");
        dept1.setDeptName("养护科A");
        dept1.setDeptType(2);
        dept1.setSortOrder(1);
        dept1.setStatus(1);
        departmentService.createDepartment(dept1);

        Department dept2 = new Department();
        dept2.setStoreId(1L);
        dept2.setDeptCode("DEPT_006");
        dept2.setDeptName("检测科A");
        dept2.setDeptType(3);
        dept2.setSortOrder(2);
        dept2.setStatus(1);
        departmentService.createDepartment(dept2);

        var list = departmentService.listByStoreId(1L);
        assertTrue(list.size() >= 2);
    }
}
