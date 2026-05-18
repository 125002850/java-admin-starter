package com.demo.core.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import com.demo.core.exception.CommonErrorCode;

class RTests {

    @Test
    void ok_without_data_should_wrap_success_and_null_payload() {
        R<Void> result = R.ok();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("ok");
        assertThat(result.getData()).isNull();
    }

    @Test
    void ok_should_wrap_data_with_code_200() {
        R<String> result = R.ok("value");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("ok");
        assertThat(result.getData()).isEqualTo("value");
    }

    @Test
    void fail_should_wrap_error_code_and_null_data() {
        R<Void> result = R.fail(CommonErrorCode.FAILED);

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).isEqualTo("操作失败");
        assertThat(result.getData()).isNull();
    }

    @Test
    void fail_with_override_msg_should_keep_error_code_and_override_message() {
        R<Void> result = R.fail(CommonErrorCode.PARAM_ERROR, "content不能为空");

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMsg()).isEqualTo("content不能为空");
        assertThat(result.getData()).isNull();
    }
}
