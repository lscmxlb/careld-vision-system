package com.careld.common.security;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT工具测试
 */
public class JwtUtilTest {

    // 仅用于测试的密钥，非生产密钥。长度需满足 HMAC-SHA256 要求（>= 32字节）
    private static final String SECRET = "test-jwt-secret-key-for-unit-testing-only-0123456789abcdef";

    @Test
    void testGenerateAndParseToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("username", "admin");
        claims.put("userType", 1);

        String token = JwtUtil.generateAccessToken(SECRET, claims);
        assertNotNull(token);
        assertTrue(JwtUtil.validateToken(SECRET, token));

        var parsedClaims = JwtUtil.parseToken(SECRET, token);
        // JWT 反序列化小数值可能返回 Integer 而非 Long，使用 Number 比较
        assertEquals(1L, ((Number) parsedClaims.get("userId")).longValue());
        assertEquals("admin", parsedClaims.get("username"));
    }

    @Test
    void testGetUserIdFromToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 123L);

        String token = JwtUtil.generateAccessToken(SECRET, claims);
        Long userId = JwtUtil.getUserId(SECRET, token);
        assertEquals(123L, userId);
    }

    @Test
    void testInvalidToken() {
        assertFalse(JwtUtil.validateToken(SECRET, "invalid.token.here"));
    }
}
