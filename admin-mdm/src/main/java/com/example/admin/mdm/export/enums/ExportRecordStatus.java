package com.example.admin.mdm.export.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.admin.core.enums.BaseEnum;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ExportRecordStatus implements BaseEnum {

    PROCESSING(1, "处理中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败"),
    EXPIRED(4, "已过期"),
    DELETED(5, "已删除");

    @EnumValue
    private final int code;
    private final String desc;

    ExportRecordStatus(int code, String desc) {
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
    public static ExportRecordStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ExportRecordStatus status : values()) {
            if (String.valueOf(status.code).equals(code)) {
                return status;
            }
        }
        return null;
    }

    public int getIntCode() {
        return code;
    }
}
