package com.careld.common.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 脱敏工具测试
 */
public class MaskUtilTest {

    @Test
    void testMaskPhone() {
        assertEquals("138****5678", MaskUtil.maskPhone("13812345678"));
        assertNull(MaskUtil.maskPhone(null));
        assertEquals("123", MaskUtil.maskPhone("123"));
    }

    @Test
    void testMaskName() {
        assertEquals("张*明", MaskUtil.maskName("张小明"));
        assertEquals("*明", MaskUtil.maskName("小明"));
        assertEquals("张", MaskUtil.maskName("张"));
        assertNull(MaskUtil.maskName(null));
    }

    @Test
    void testMaskIdCard() {
        assertEquals("110101********1234", MaskUtil.maskIdCard("110101199001011234"));
        assertNull(MaskUtil.maskIdCard(null));
    }
}
