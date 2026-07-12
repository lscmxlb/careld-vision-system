package com.careld.common.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AES加密工具测试
 */
public class AesUtilTest {

    // 仅用于测试的密钥，非生产密钥。AES-256 需要恰好 32 字节
    private static final String SECRET_KEY = "careld-vision-test-key-32bytes!!"; // 恰好 32 字符

    @Test
    void testEncryptDecrypt() {
        String plaintext = "张小明";
        String encrypted = AesUtil.encrypt(plaintext, SECRET_KEY);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);

        String decrypted = AesUtil.decrypt(encrypted, SECRET_KEY);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testEncryptNull() {
        assertNull(AesUtil.encrypt(null, SECRET_KEY));
        assertNull(AesUtil.encrypt("", SECRET_KEY));
    }

    @Test
    void testDecryptNull() {
        assertNull(AesUtil.decrypt(null, SECRET_KEY));
        assertNull(AesUtil.decrypt("", SECRET_KEY));
    }

    @Test
    void testEncryptPhone() {
        String phone = "13812345678";
        String encrypted = AesUtil.encrypt(phone, SECRET_KEY);
        String decrypted = AesUtil.decrypt(encrypted, SECRET_KEY);
        assertEquals(phone, decrypted);
    }
}
