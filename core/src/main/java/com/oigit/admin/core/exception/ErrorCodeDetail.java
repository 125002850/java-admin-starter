package com.oigit.admin.core.exception;

import java.util.Objects;

/** 命名错误定义的参数化提示；保留基础错误码身份。业务入口调用所属模块的错误工厂。 */
public record ErrorCodeDetail(ErrorCode errorCode, String message) implements ErrorCode {

    public ErrorCodeDetail {
        Objects.requireNonNull(errorCode, "errorCode");
        Objects.requireNonNull(message, "message");
        if (errorCode instanceof ErrorCodeDetail detail) {
            errorCode = detail.errorCode();
        }
    }

    @Override
    public int getCode() {
        return errorCode.getCode();
    }

    @Override
    public String getMsg() {
        return message;
    }

}
