package com.oigit.admin.iam.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oigit.admin.core.enums.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum IamStatus implements BaseEnum {

    ENABLED("ENABLED", "启用"),
    DISABLED("DISABLED", "禁用");

    @EnumValue
    private final String code;
    private final String desc;

    IamStatus(String code, String desc) {
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
    public static IamStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (IamStatus item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
