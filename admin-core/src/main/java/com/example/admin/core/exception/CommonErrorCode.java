package com.example.admin.core.exception;

public enum CommonErrorCode implements ErrorCode {

    SUCCESS(200, "ok"),

    FAILED(500, "操作失败"),

    PARAM_ERROR(400, "参数错误"),

    UNAUTHORIZED(401, "未登录"),

    FORBIDDEN(403, "无权限"),

    NOT_FOUND(404, "资源不存在"),

    TOO_MANY_REQUESTS(429, "请求过于频繁");


    private final int code;
    private final String msg;

    CommonErrorCode(int code, String msg) {
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
