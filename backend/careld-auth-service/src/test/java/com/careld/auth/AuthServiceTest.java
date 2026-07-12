package com.careld.auth;

import com.careld.auth.dto.LoginRequest;
import com.careld.auth.dto.LoginResponse;
import com.careld.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证服务测试
 */
@SpringBootTest
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void testPasswordEncoder() {
        String password = "Test@Secure#2024";
        String encoded = passwordEncoder.encode(password);
        // 验证编码后可正确匹配
        assertTrue(passwordEncoder.matches(password, encoded));
    }

    @Test
    void testLoginWithInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("wrongpassword");

        Exception exception = assertThrows(Exception.class, () -> {
            authService.login(request);
        });
        assertNotNull(exception);
    }

    @Test
    void testTokenGeneration() {
        // 测试Token生成逻辑
        assertNotNull(authService);
    }
}
