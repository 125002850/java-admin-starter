package com.demo.core.web;

import com.demo.core.exception.CommonErrorCode;
import com.demo.core.exception.ErrorCode;

public final class R<T> {

    private final int code;
    private final String msg;
    private final T data;

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static R<Void> ok() {
        return new R<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMsg(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String msg) {
        return new R<>(errorCode.getCode(), msg, null);
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }
}
