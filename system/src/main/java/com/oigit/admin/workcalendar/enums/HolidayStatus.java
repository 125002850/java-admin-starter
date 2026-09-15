package com.oigit.admin.workcalendar.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;

@com.oigit.admin.core.enums.DictionaryEnum("WORK_CALENDAR_HOLIDAY_STATUS")
public enum HolidayStatus implements BaseEnum {

    CONFIRMED("confirmed", "已确认"),
    CANDIDATE("candidate", "草稿候选"),
    UNKNOWN("unknown", "未知");

    @EnumValue
    private final String code;
    private final String desc;

    HolidayStatus(String code, String desc) {
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
    public static HolidayStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (HolidayStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
