package com.oigit.admin.core.audit;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;

@com.oigit.admin.core.enums.DictionaryEnum("OPERATION_AUDIT_STATUS")
public enum OperationAuditResultStatus implements BaseEnum {

    SUCCESS("success", "成功"),
    FAILED("failed", "失败");

    @EnumValue
    private final String code;
    private final String desc;

    OperationAuditResultStatus(String code, String desc) {
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
    public static OperationAuditResultStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OperationAuditResultStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
