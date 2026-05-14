package com.demo.core.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RTests {

    @Test
    void ok_should_wrap_data_with_code_200() {
        R<String> result = R.ok("value");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("ok");
        assertThat(result.getData()).isEqualTo("value");
    }
}
