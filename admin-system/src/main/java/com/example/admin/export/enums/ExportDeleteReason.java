package com.example.admin.export.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.admin.core.enums.BaseEnum;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ExportDeleteReason implements BaseEnum {

    MANUAL(1, "手动删除"),
    EXPIRED_CLEANUP(2, "过期清理");

    @EnumValue
    private final int code;
    private final String desc;

    ExportDeleteReason(int code, String desc) {
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
    public static ExportDeleteReason fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ExportDeleteReason reason : values()) {
            if (String.valueOf(reason.code).equals(code)) {
                return reason;
            }
        }
        return null;
    }

    public int getIntCode() {
        return code;
    }
}
