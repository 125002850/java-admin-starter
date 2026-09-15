package com.oigit.admin.workcalendar.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;

@com.oigit.admin.core.enums.DictionaryEnum("WORK_CALENDAR_YEAR_VIEW")
public enum WorkCalendarYearView implements BaseEnum {

    DRAFT("draft", "草稿"),
    ACTIVE("active", "当前生效版本");

    @EnumValue
    private final String code;
    private final String desc;

    WorkCalendarYearView(String code, String desc) {
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
    public static WorkCalendarYearView fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (WorkCalendarYearView value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
