package com.oigit.admin.core.exception;

public interface ErrorCode {

    int getCode();

    String getMsg();

    /** 业务失败默认保持 HTTP 200；明确约定传输状态的错误码可覆盖。 */
    default int getHttpStatus() {
        return 200;
    }
}
