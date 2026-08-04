package com.oigit.admin.iam.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;
import com.oigit.admin.core.enums.DictionaryEnum;

@DictionaryEnum("IAM_LOGIN_EVENT_TYPE")
public enum LoginEventType implements BaseEnum {
    LOGIN("LOGIN", "登录"),
    REFRESH("REFRESH", "刷新令牌"),
    LOGOUT("LOGOUT", "退出登录");

    @EnumValue
    private final String code;
    private final String desc;

    LoginEventType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static LoginEventType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LoginEventType item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
