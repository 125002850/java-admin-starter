package com.oigit.admin.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 启用状态枚举
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnableStatusEnum implements BaseEnum {

    ENABLE("enable", "启用"),
    DISABLE("disable", "禁用");

    @EnumValue
    private final String code;
    private final String desc;

    EnableStatusEnum(String code, String desc) {
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
    public static EnableStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (EnableStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
