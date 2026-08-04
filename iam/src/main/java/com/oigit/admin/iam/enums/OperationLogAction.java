package com.oigit.admin.iam.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;
import com.oigit.admin.core.enums.DictionaryEnum;

@DictionaryEnum("IAM_OPERATION_LOG_ACTION")
public enum OperationLogAction implements BaseEnum {
    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "编辑"),
    DELETE("DELETE", "删除"),
    STATUS_UPDATE("STATUS_UPDATE", "状态变更"),
    ASSIGN("ASSIGN", "分配"),
    RESET_PASSWORD("RESET_PASSWORD", "重置密码"),
    CHANGE_PASSWORD("CHANGE_PASSWORD", "修改密码"),
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "退出");

    @EnumValue
    private final String code;
    private final String desc;

    OperationLogAction(String code, String desc) {
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
    public static OperationLogAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OperationLogAction item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
