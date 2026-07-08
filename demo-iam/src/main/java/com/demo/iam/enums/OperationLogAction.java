package com.demo.iam.enums;

public enum OperationLogAction {
    CREATE("新增"),
    UPDATE("编辑"),
    DELETE("删除"),
    STATUS_UPDATE("状态变更"),
    ASSIGN("分配"),
    RESET_PASSWORD("重置密码"),
    CHANGE_PASSWORD("修改密码"),
    LOGIN("登录"),
    LOGOUT("退出");

    private final String desc;

    OperationLogAction(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
