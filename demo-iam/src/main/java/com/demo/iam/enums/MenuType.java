package com.demo.iam.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.demo.core.enums.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MenuType implements BaseEnum {

    DIR("DIR", "目录"),
    MENU("MENU", "菜单"),
    BUTTON("BUTTON", "按钮");

    @EnumValue
    private final String code;
    private final String desc;

    MenuType(String code, String desc) {
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
    public static MenuType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MenuType item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
