package com.demo.system.enums;

public enum UserStatusEnum {

    ENABLED(1, "启用"),
    DISABLED(0, "禁用");

    private final int value;
    private final String desc;

    UserStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    public static UserStatusEnum fromEnabled(boolean enabled) {
        return enabled ? ENABLED : DISABLED;
    }
}
