package com.oigit.admin.core.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnableStatusEnumTests {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void shouldSerializeAsObject() throws Exception {
        String json = objectMapper.writeValueAsString(EnableStatusEnum.ENABLE);

        assertThat(json).contains("\"code\"");
        assertThat(json).contains("\"desc\"");
        assertThat(json).contains("\"enable\"");
        assertThat(json).contains("\"启用\"");
    }

    @Test
    void shouldDeserializeFromCodeString() throws Exception {
        EnableStatusEnum result = objectMapper.readValue("\"enable\"", EnableStatusEnum.class);

        assertThat(result).isEqualTo(EnableStatusEnum.ENABLE);
    }

    @Test
    void fromCodeShouldWorkDirectly() {
        assertThat(EnableStatusEnum.fromCode("enable")).isEqualTo(EnableStatusEnum.ENABLE);
        assertThat(EnableStatusEnum.fromCode("disable")).isEqualTo(EnableStatusEnum.DISABLE);
        assertThat(EnableStatusEnum.fromCode(null)).isNull();
        assertThat(EnableStatusEnum.fromCode("unknown")).isNull();
    }

    @Test
    void getCodeAndDescShouldReturnExpectedValues() {
        assertThat(EnableStatusEnum.ENABLE.getCode()).isEqualTo("enable");
        assertThat(EnableStatusEnum.ENABLE.getDesc()).isEqualTo("启用");
        assertThat(EnableStatusEnum.DISABLE.getCode()).isEqualTo("disable");
        assertThat(EnableStatusEnum.DISABLE.getDesc()).isEqualTo("禁用");
    }
}
