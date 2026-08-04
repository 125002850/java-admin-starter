package com.oigit.admin.iam.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;
import com.oigit.admin.core.enums.DictionaryEnum;

@DictionaryEnum("IAM_LOGIN_RESULT")
public enum LoginResult implements BaseEnum {
    SUCCESS("SUCCESS", "成功"),
    FAIL("FAIL", "失败");

    @EnumValue
    private final String code;
    private final String desc;

    LoginResult(String code, String desc) {
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
    public static LoginResult fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LoginResult item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
