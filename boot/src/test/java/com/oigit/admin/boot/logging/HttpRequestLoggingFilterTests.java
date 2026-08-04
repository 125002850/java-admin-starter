package com.oigit.admin.boot.logging;

import com.oigit.admin.core.web.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "platform.http-logging.enabled=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(HttpRequestLoggingFilterTests.HttpLoggingTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class HttpRequestLoggingFilterTests {

    private final MockMvc mockMvc;

    @Autowired
    HttpRequestLoggingFilterTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldLogCompletedRequestAndMaskSensitivePayload(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/http-log")
                        .queryParam("keyword", "erp")
                        .queryParam("access_token", "query-secret")
                        .header("X-Trace-Id", "trace-http-log-1")
                        .header("X-User-Id", "10001")
                        .header("X-Forwarded-For", "10.0.0.8, 10.0.0.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"export\",\"password\":\"request-secret\",\"mobile\":\"13800138000\"}"))
                .andExpect(status().isOk());

        assertThat(output.getOut())
                .contains("REQ DONE")
                .contains("| method  : POST")
                .contains("| uri     : /test/http-log")
                .contains("| traceId : trace-http-log-1")
                .contains("| userId  : 10001")
                .contains("| ip      : 10.0.0.8")
                .contains("| query   : keyword=erp&access_token=***")
                .contains("\"password\":\"***\"")
                .contains("\"mobile\":\"***\"")
                .contains("\"accessToken\":\"***\"")
                .contains("| status  : 200")
                .containsPattern("\\| cost    : \\d+ms")
                .doesNotContain("request-secret", "response-secret", "13800138000", "query-secret");
    }

    @Test
    void shouldLogGatewayRejectedRequest(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/http-log")
                        .header("X-Trace-Id", "trace-http-log-invalid-user")
                        .header("X-User-Id", "not-a-number")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(output.getOut())
                .contains("| traceId : trace-http-log-invalid-user")
                .contains("| userId  : not-a-number")
                .contains("| status  : 400");
    }

    @Test
    void shouldExcludeHealthEndpoint(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Trace-Id", "trace-health-excluded"))
                .andExpect(status().isOk());

        assertThat(output.getOut()).doesNotContain("| traceId : trace-health-excluded");
    }

    @TestConfiguration
    static class HttpLoggingTestConfig {

        @Bean
        HttpLoggingTestController httpLoggingTestController() {
            return new HttpLoggingTestController();
        }
    }

    @RestController
    static class HttpLoggingTestController {

        @PostMapping("/test/http-log")
        R<Map<String, Object>> echo(@RequestBody Map<String, Object> body) {
            Map<String, Object> response = new LinkedHashMap<>(body);
            response.put("accessToken", "response-secret");
            return R.ok(response);
        }
    }
}
