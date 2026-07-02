package com.demo.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 是否枚举
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum YesNoEnum implements BaseEnum {

    YES(1, "是"),
    NO(0, "否");

    @EnumValue
    private final int code;
    private final String desc;

    YesNoEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static YesNoEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (YesNoEnum e : values()) {
            if (String.valueOf(e.code).equals(code)) {
                return e;
            }
        }
        return null;
    }

    public int getIntCode() {
        return code;
    }
}
