package com.oigit.admin.core.audit;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.oigit.admin.core.enums.BaseEnum;

@com.oigit.admin.core.enums.DictionaryEnum("OPERATION_AUDIT_ACTION")
public enum OperationAuditAction implements BaseEnum {

    CREATE("create", "新增"),
    UPDATE("update", "修改"),
    DELETE("delete", "删除"),
    STATUS_CHANGE("status_change", "状态变更"),
    CONFIRM("confirm", "确认"),
    PUBLISH("publish", "发布"),
    ARCHIVE("archive", "归档"),
    SYNC("sync", "同步"),
    IMPORT("import", "导入"),
    EXPORT("export", "导出"),
    UPLOAD("upload", "上传"),
    DOWNLOAD("download", "下载"),
    EXECUTE("execute", "执行");

    @EnumValue
    private final String code;
    private final String desc;

    OperationAuditAction(String code, String desc) {
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
    public static OperationAuditAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OperationAuditAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return null;
    }
}
