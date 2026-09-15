package com.oigit.admin.workcalendar.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;

@com.oigit.admin.core.enums.DictionaryEnum("WORK_CALENDAR_VERSION_STATUS")
public enum WorkCalendarVersionStatus implements BaseEnum {

    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布");

    @EnumValue
    private final String code;
    private final String desc;

    WorkCalendarVersionStatus(String code, String desc) {
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
    public static WorkCalendarVersionStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (WorkCalendarVersionStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
