package com.oigit.admin.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HttpLogSanitizerTests {

    private final HttpLoggingProperties properties = new HttpLoggingProperties();
    private final HttpLogSanitizer sanitizer = new HttpLogSanitizer(
            new ObjectMapper(),
            properties.getSensitiveFields()
    );

    @Test
    void shouldMaskSensitiveJsonFieldsRecursively() {
        String body = """
                {"name":"测试系统","password":"plain","nested":{"accessToken":"token-value"},
                "users":[{"mobile":"13800138000","userId":10}]}
                """;

        String sanitized = sanitizer.sanitizeBody(
                body.getBytes(StandardCharsets.UTF_8),
                "application/json",
                "UTF-8",
                true,
                false,
                8192
        );

        assertThat(sanitized)
                .contains("\"name\":\"测试系统\"")
                .contains("\"password\":\"***\"")
                .contains("\"accessToken\":\"***\"")
                .contains("\"mobile\":\"***\"")
                .contains("\"userId\":10")
                .doesNotContain("plain", "token-value", "13800138000");
    }

    @Test
    void shouldMaskSensitiveQueryValuesAndKeepBusinessValues() {
        String sanitized = sanitizer.sanitizeQuery("keyword=export&access_token=secret&mobile=13800138000");

        assertThat(sanitized).isEqualTo("keyword=export&access_token=***&mobile=***");
    }

    @Test
    void shouldOmitOversizedAndBinaryBodies() {
        assertThat(sanitizer.sanitizeBody(
                "oversized".getBytes(StandardCharsets.UTF_8),
                "application/json",
                "UTF-8",
                true,
                true,
                4
        )).isEqualTo("<omitted: body exceeds 4 bytes>");

        assertThat(sanitizer.sanitizeBody(
                new byte[]{1, 2, 3},
                "application/octet-stream",
                "UTF-8",
                true,
                false,
                8192
        )).isEqualTo("<omitted: content-type=application/octet-stream>");
    }
}
