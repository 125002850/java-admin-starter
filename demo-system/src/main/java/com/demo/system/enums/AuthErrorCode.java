package com.demo.system.enums;

import com.demo.core.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

    USERNAME_OR_PASSWORD_INVALID(2001001, "用户名或密码错误"),
    USERNAME_DUPLICATED(2001002, "用户名重复，请联系管理员处理");

    private final int code;
    private final String message;

    AuthErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
