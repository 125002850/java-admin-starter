package com.example.admin.iam.enums;

import com.example.admin.core.exception.ErrorCode;

public enum IamErrorCode implements ErrorCode {

    AUTH_BAD_CREDENTIALS(2001001, "用户名或密码错误"),
    AUTH_STAFF_DISABLED(2001002, "员工已禁用"),
    AUTH_STAFF_NOT_FOUND(2001003, "员工不存在"),
    AUTH_REFRESH_TOKEN_INVALID(2001004, "refresh token 无效"),
    AUTH_REFRESH_TOKEN_EXPIRED(2001005, "refresh token 已过期"),
    AUTH_PASSWORD_POLICY_INVALID(2001006, "密码不符合策略"),
    AUTH_MUST_CHANGE_PASSWORD(2001007, "必须修改密码"),
    AUTH_OLD_PASSWORD_INVALID(2001008, "旧密码错误"),

    STAFF_USERNAME_DUPLICATED(2002001, "用户名重复"),
    STAFF_CODE_DUPLICATED(2002002, "员工工号重复"),
    STAFF_NOT_FOUND(2002003, "员工不存在"),
    STAFF_SUPER_ADMIN_REQUIRED(2002004, "系统必须至少保留一个启用的超级管理员员工"),
    STAFF_OUT_OF_DATA_SCOPE(2002005, "目标员工不在当前用户的数据权限范围内"),

    DEPT_NOT_FOUND(2003001, "部门不存在"),
    DEPT_CODE_DUPLICATED(2003002, "部门编码重复"),
    DEPT_NAME_DUPLICATED(2003003, "部门名称重复"),
    DEPT_HAS_CHILDREN(2003004, "部门下存在子部门"),
    DEPT_HAS_STAFF(2003005, "部门下存在员工"),
    DEPT_DISABLED(2003006, "部门已禁用"),
    DEPT_PARENT_INVALID(2003007, "部门父节点非法"),

    ROLE_NOT_FOUND(2004001, "角色不存在"),
    ROLE_CODE_DUPLICATED(2004002, "角色编码重复"),
    ROLE_NAME_DUPLICATED(2004003, "角色名称重复"),
    ROLE_SUPER_ADMIN_PROTECTED(2004004, "不能删除、禁用或收窄 SUPER_ADMIN 角色"),
    ROLE_HAS_STAFF(2004005, "角色下存在员工绑定"),

    MENU_NOT_FOUND(2005001, "菜单不存在"),
    MENU_CODE_DUPLICATED(2005002, "菜单编码重复"),
    MENU_PERMISSION_DUPLICATED(2005003, "权限标识重复"),
    MENU_HAS_CHILDREN(2005004, "菜单下存在子节点"),
    MENU_BUTTON_PERMISSION_REQUIRED(2005005, "按钮权限必须配置权限标识"),
    MENU_PARENT_INVALID(2005006, "菜单父节点非法");

    private final int code;
    private final String msg;

    IamErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
