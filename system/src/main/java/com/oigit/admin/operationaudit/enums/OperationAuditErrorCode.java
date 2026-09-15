package com.oigit.admin.operationaudit.enums;

import com.oigit.admin.core.exception.ErrorCode;

public enum OperationAuditErrorCode implements ErrorCode {

    LOG_NOT_FOUND(3011001, "操作日志不存在");

    private final int code;
    private final String msg;

    OperationAuditErrorCode(int code, String msg) {
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
