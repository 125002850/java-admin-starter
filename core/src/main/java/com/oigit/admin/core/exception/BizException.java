package com.oigit.admin.core.exception;

public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode instanceof ErrorCodeDetail detail ? detail.errorCode() : errorCode;
    }

    /** 历史调用兼容；新业务通过模块命名错误工厂构造 ErrorCodeDetail。 */
    public BizException(ErrorCode errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode instanceof ErrorCodeDetail detail ? detail.errorCode() : errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
