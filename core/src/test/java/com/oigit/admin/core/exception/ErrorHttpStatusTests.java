package com.oigit.admin.core.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorHttpStatusTests {
    @Test
    void preservesDefaultAndExplicitHttpSemanticsThroughDetailedErrors() {
        var handler = new GlobalExceptionHandler();
        assertThat(handler.handleBizException(new BizException(CommonErrorCode.FAILED))
                .getStatusCode().value()).isEqualTo(200);
        ErrorCode conflict = new ErrorCode() {
            public int getCode() { return 3001999; }
            public String getMsg() { return "版本冲突"; }
            public int getHttpStatus() { return 409; }
        };
        var response = handler.handleBizException(new BizException(new ErrorCodeDetail(conflict, "请重新读取版本")));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getCode()).isEqualTo(3001999);
        assertThat(response.getBody().getMsg()).isEqualTo("请重新读取版本");
    }
}
