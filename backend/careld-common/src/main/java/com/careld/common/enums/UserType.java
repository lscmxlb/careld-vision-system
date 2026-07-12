package com.careld.common.enums;

import lombok.Getter;

/**
 * 用户类型枚举
 */
@Getter
public enum UserType {

    HEADQUARTERS(1, "总部运营"),
    STORE_STAFF(2, "门店医护"),
    PARENT(3, "家长");

    private final Integer code;
    private final String desc;

    UserType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserType of(Integer code) {
        for (UserType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
