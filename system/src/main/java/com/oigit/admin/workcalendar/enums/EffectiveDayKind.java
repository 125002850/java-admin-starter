package com.oigit.admin.workcalendar.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;

@com.oigit.admin.core.enums.DictionaryEnum("WORK_CALENDAR_EFFECTIVE_DAY_KIND")
public enum EffectiveDayKind implements BaseEnum {

    REGULAR_WORKDAY("regular_workday", "普通工作日"),
    ADJUSTED_WORKDAY("adjusted_workday", "调休工作日"),
    WEEKEND("weekend", "周末"),
    PUBLIC_HOLIDAY("public_holiday", "法定节假日"),
    OTHER_NON_WORKING_DAY("other_non_working_day", "其他非工作日");

    @EnumValue
    private final String code;
    private final String desc;

    EffectiveDayKind(String code, String desc) {
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
    public static EffectiveDayKind fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (EffectiveDayKind value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
