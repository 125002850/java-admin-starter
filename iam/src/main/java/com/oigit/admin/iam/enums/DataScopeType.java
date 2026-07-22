package com.oigit.admin.iam.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oigit.admin.core.enums.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DataScopeType implements BaseEnum {

    ALL("ALL", "全部数据"),
    DEPT_AND_CHILD("DEPT_AND_CHILD", "本部门及子部门"),
    DEPT_ONLY("DEPT_ONLY", "本部门"),
    SELF("SELF", "仅本人"),
    CUSTOM_DEPT("CUSTOM_DEPT", "自定义部门"),
    MIXED("MIXED", "混合范围");

    @EnumValue
    private final String code;
    private final String desc;

    DataScopeType(String code, String desc) {
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
    public static DataScopeType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataScopeType item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
