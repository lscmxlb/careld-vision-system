package com.careld.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
public class JwtUtil {

    private static final long ACCESS_TOKEN_EXPIRE = 7200 * 1000; // 2小时
    private static final long REFRESH_TOKEN_EXPIRE = 7 * 24 * 3600 * 1000; // 7天

    /**
     * 生成访问令牌
     */
    public static String generateAccessToken(String secret, Map<String, Object> claims) {
        return generateToken(secret, claims, ACCESS_TOKEN_EXPIRE);
    }

    /**
     * 生成刷新令牌
     */
    public static String generateRefreshToken(String secret, Map<String, Object> claims) {
        return generateToken(secret, claims, REFRESH_TOKEN_EXPIRE);
    }

    /**
     * 生成令牌
     */
    public static String generateToken(String secret, Map<String, Object> claims, long expire) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expire);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌
     */
    public static Claims parseToken(String secret, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证令牌
     */
    public static boolean validateToken(String secret, String token) {
        try {
            parseToken(secret, token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            return false;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId(String secret, String token) {
        Claims claims = parseToken(secret, token);
        Object userId = claims.get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    /**
     * 获取用户类型
     */
    public static Integer getUserType(String secret, String token) {
        Claims claims = parseToken(secret, token);
        Object userType = claims.get("userType");
        return userType != null ? Integer.valueOf(userType.toString()) : null;
    }
}
