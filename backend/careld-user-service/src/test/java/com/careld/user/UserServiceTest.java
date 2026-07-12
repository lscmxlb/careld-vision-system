package com.careld.user;

import com.careld.user.dto.UserCreateRequest;
import com.careld.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试
 */
@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testUserServiceExists() {
        assertNotNull(userService);
    }

    @Test
    void testUserCreateRequestDto() {
        // 验证 DTO 构造和字段赋值正确
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("test_user");
        request.setPassword("Test@Secure#2024");
        request.setRealName("测试用户");
        request.setUserType(2);

        assertEquals("test_user", request.getUsername());
        assertEquals("测试用户", request.getRealName());
        assertEquals(2, request.getUserType());
    }
}
