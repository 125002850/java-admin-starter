package com.demo.system.enums;

import com.demo.core.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(2003001, "用户不存在"),
    USERNAME_DUPLICATED(2003002, "用户名已存在");

    private final int code;
    private final String msg;

    UserErrorCode(int code, String msg) {
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
