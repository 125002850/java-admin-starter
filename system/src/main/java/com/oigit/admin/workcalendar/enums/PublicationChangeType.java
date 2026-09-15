package com.oigit.admin.workcalendar.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;

@com.oigit.admin.core.enums.DictionaryEnum("WORK_CALENDAR_PUBLICATION_CHANGE_TYPE")
public enum PublicationChangeType implements BaseEnum {

    ADDED("added", "新增"),
    REMOVED("removed", "删除"),
    CHANGED("changed", "修改");

    @EnumValue
    private final String code;
    private final String desc;

    PublicationChangeType(String code, String desc) {
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
    public static PublicationChangeType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PublicationChangeType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
