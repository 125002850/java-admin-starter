package com.oigit.admin.core.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeDetailTests {
    @Test void nestedDetailsKeepCodeIdentityAndLatestMessage() {
        var detail = new ErrorCodeDetail(new ErrorCodeDetail(CommonErrorCode.FAILED, "旧提示"), "新提示");
        var exception = new BizException(detail);
        assertThat(exception.getErrorCode()).isSameAs(CommonErrorCode.FAILED);
        assertThat(exception.getMessage()).isEqualTo("新提示");
        assertThat(detail.getCode()).isEqualTo(CommonErrorCode.FAILED.getCode());
        assertThat(new BizException(CommonErrorCode.FAILED, "兼容提示").getMessage()).isEqualTo("兼容提示");
    }
}
