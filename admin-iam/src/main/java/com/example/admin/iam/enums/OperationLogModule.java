package com.example.admin.iam.enums;

public enum OperationLogModule {
    IAM_AUTH("IAM认证"),
    IAM_STAFF("员工管理"),
    IAM_DEPT("部门管理"),
    IAM_ROLE("角色管理"),
    IAM_MENU("菜单管理");

    private final String desc;

    OperationLogModule(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
