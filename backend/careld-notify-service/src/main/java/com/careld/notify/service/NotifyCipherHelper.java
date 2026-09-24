package com.careld.notify.service;

import com.careld.common.security.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 儿童姓名/手机号的加解密与匹配（沿用 child_profile 的 AES 存储约定，
 * 兼容种子数据的 "ENC:明文" 占位格式）
 */
@Slf4j
@Component
public class NotifyCipherHelper {

    private static final String PLACEHOLDER_PREFIX = "ENC:";

    @Value("${encryption.key:careld-vision-encrypt-key-32byte}")
    private String encryptionKey;

    /** 解密；密文缺失/解密失败返回 null（调用方保留脱敏值） */
    public String decrypt(String ciphertext) {
        if (!StringUtils.hasText(ciphertext)) {
            return null;
        }
        if (ciphertext.startsWith(PLACEHOLDER_PREFIX)) {
            return ciphertext.substring(PLACEHOLDER_PREFIX.length());
        }
        try {
            return AesUtil.decrypt(ciphertext, encryptionKey);
        } catch (Exception e) {
            log.debug("解密失败，按不可用处理: {}", e.getMessage());
            return null;
        }
    }

    /** 姓名匹配：脱敏值片段命中，或解密后全名命中 */
    public boolean matchesName(String nameMask, String nameEncrypted, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return false;
        }
        if (nameMask != null && nameMask.contains(keyword)) {
            return true;
        }
        String plainName = decrypt(nameEncrypted);
        return plainName != null && plainName.contains(keyword);
    }
}
