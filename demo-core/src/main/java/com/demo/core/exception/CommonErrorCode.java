package com.demo.core.exception;

public enum CommonErrorCode implements ErrorCode {

    SUCCESS(200, "ok"),

    FAILED(500, "操作失败"),

    PARAM_ERROR(400, "参数错误"),

    UNAUTHORIZED(401, "未登录"),

    FORBIDDEN(403, "无权限"),

    NOT_FOUND(404, "资源不存在"),

    TOO_MANY_REQUESTS(429, "请求过于频繁");


    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) {
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
