package com.oigit.admin.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountLogSanitizerTests {
    @Test
    void redactsAccountAndCertificateNumbersRecursively() {
        var sanitizer = new HttpLogSanitizer(new ObjectMapper(), Set.of());
        var body = "{\"nested\":{\"bankAccountNo\":\"private-account\",\"certificateNo\":\"private-certificate\",\"name\":\"ok\"}}";
        var result = sanitizer.sanitizeBody(body.getBytes(StandardCharsets.UTF_8), "application/json", "UTF-8", true, false, 4096);
        assertThat(result).contains("ok").doesNotContain("private-account", "private-certificate");
    }
}
