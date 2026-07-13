package com.example.admin.iam.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.example.admin.core.enums.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LoginFailureReason implements BaseEnum {

    BAD_CREDENTIALS("BAD_CREDENTIALS", "用户名或密码错误"),
    STAFF_DISABLED("STAFF_DISABLED", "员工已禁用"),
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID", "刷新令牌无效"),
    REFRESH_TOKEN_EXPIRED("REFRESH_TOKEN_EXPIRED", "刷新令牌已过期");

    @EnumValue
    private final String code;
    private final String desc;

    LoginFailureReason(String code, String desc) {
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
    public static LoginFailureReason fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LoginFailureReason item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
