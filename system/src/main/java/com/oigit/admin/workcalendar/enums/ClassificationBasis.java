package com.oigit.admin.workcalendar.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;

@com.oigit.admin.core.enums.DictionaryEnum("WORK_CALENDAR_CLASSIFICATION_BASIS")
public enum ClassificationBasis implements BaseEnum {

    PUBLISHED_SNAPSHOT("published_snapshot", "已发布年度快照"),
    WEEKLY_FALLBACK("weekly_fallback", "每周规则临时推导"),
    SAVED_DRAFT("saved_draft", "已保存草稿");

    @EnumValue
    private final String code;
    private final String desc;

    ClassificationBasis(String code, String desc) {
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
    public static ClassificationBasis fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ClassificationBasis value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
