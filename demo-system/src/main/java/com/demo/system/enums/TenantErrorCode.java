package com.demo.system.enums;

import com.demo.core.exception.ErrorCode;

public enum TenantErrorCode implements ErrorCode {

    TENANT_NOT_FOUND(2002001, "租户不存在"),
    TENANT_NAME_DUPLICATED(2002002, "租户名称已存在"),
    TENANT_HAS_USERS(2002003, "租户下存在未删除用户，不能删除");

    private final int code;
    private final String msg;

    TenantErrorCode(int code, String msg) {
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
