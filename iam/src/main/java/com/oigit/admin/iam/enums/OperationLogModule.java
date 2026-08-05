package com.oigit.admin.iam.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;
import com.oigit.admin.core.enums.DictionaryEnum;

@DictionaryEnum("IAM_OPERATION_LOG_MODULE")
public enum OperationLogModule implements BaseEnum {
    IAM_AUTH("IAM_AUTH", "IAM认证"),
    IAM_STAFF("IAM_STAFF", "员工管理"),
    IAM_DEPT("IAM_DEPT", "部门管理"),
    IAM_ROLE("IAM_ROLE", "角色管理"),
    IAM_MENU("IAM_MENU", "菜单管理");

    @EnumValue
    private final String code;
    private final String desc;

    OperationLogModule(String code, String desc) {
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
    public static OperationLogModule fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OperationLogModule item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
