package com.demo.core.web;

public class R<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> result = new R<>();
        result.code = 200;
        result.msg = "ok";
        result.data = data;
        return result;
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
