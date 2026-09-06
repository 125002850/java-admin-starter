package com.oigit.admin.boot.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.boot.iam.IamTestAuth;
import com.oigit.admin.core.web.R;
import com.oigit.admin.core.operator.OperatorContext;
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

@SpringBootTest(properties = {
        "platform.http-logging.enabled=true",
        "platform.operator.gateway-filter-enabled=true",
        "platform.iam.client-ip.trusted-proxy-cidrs[0]=10.0.0.0/24"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(HttpRequestLoggingFilterTests.HttpLoggingTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class HttpRequestLoggingFilterTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    HttpRequestLoggingFilterTests(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void shouldLogCompletedRequestAndMaskSensitivePayload(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/http-log")
                        .queryParam("keyword", "erp")
                        .queryParam("access_token", "query-secret")
                        .header("X-Trace-Id", "trace-http-log-1")
                        .header("X-User-Id", "10001")
                        .header("X-Forwarded-For", "10.0.0.8, 10.0.0.9")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"export\",\"password\":\"request-secret\",\"mobile\":\"13800138000\"}"))
                .andExpect(status().isOk());

        assertThat(output.getOut())
                .contains("REQ DONE")
                .contains("| method  : POST")
                .contains("| uri     : /test/http-log")
                .contains("| traceId : trace-http-log-1")
                .contains("| userId  : -")
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
    void shouldLogJwtIdentityAfterContextCleanupDespiteGatewayHeader(CapturedOutput output) throws Exception {
        String accessToken = IamTestAuth.adminAccessToken(mockMvc, objectMapper);
        var result = mockMvc.perform(post("/test/http-log")
                        .header("X-Trace-Id", "trace-http-log-local-user")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-User-Id", "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        Object authenticatedOperatorId = result.getRequest()
                .getAttribute(OperatorContext.REQUEST_ATTRIBUTE_OPERATOR_ID);
        assertThat(authenticatedOperatorId).isInstanceOf(Long.class).isNotEqualTo(99999L);
        assertThat(OperatorContext.getOperatorId()).isNull();
        assertThat(completionLog(output, "trace-http-log-local-user"))
                .contains("| userId  : " + authenticatedOperatorId + "\n")
                .doesNotContain("| userId  : 99999")
                .contains("| status  : 200");
    }

    @Test
    void shouldIgnoreForwardedHeadersFromUntrustedPeer(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/http-log")
                        .header("X-Trace-Id", "trace-http-log-untrusted-peer")
                        .header("X-User-Id", "99999")
                        .header("X-Forwarded-For", "198.51.100.99")
                        .header("Forwarded", "for=198.51.100.98")
                        .header("X-Real-IP", "198.51.100.97")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        assertThat(completionLog(output, "trace-http-log-untrusted-peer"))
                .contains("| userId  : -\n")
                .contains("| ip      : 203.0.113.10\n")
                .doesNotContain("198.51.100.99", "198.51.100.98", "198.51.100.97");
    }

    @Test
    void shouldStopAtFirstUntrustedAddressInTrustedProxyChain(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/http-log")
                        .header("X-Trace-Id", "trace-http-log-proxy-chain")
                        .header("X-Forwarded-For", "198.51.100.99, 203.0.113.20, 10.0.0.9")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        assertThat(completionLog(output, "trace-http-log-proxy-chain"))
                .contains("| ip      : 203.0.113.20\n")
                .doesNotContain("198.51.100.99");
    }

    @Test
    void shouldLogRejectedJwtWithoutUnverifiedIdentity(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/http-log")
                        .header("X-Trace-Id", "trace-http-log-rejected-jwt")
                        .header("Authorization", "Bearer invalid-token")
                        .header("X-User-Id", "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        assertThat(completionLog(output, "trace-http-log-rejected-jwt"))
                .contains("| userId  : -\n")
                .contains("| status  : 401\n")
                .doesNotContain("99999", "invalid-token");
    }

    @Test
    void shouldLogGatewayRejectionWithTraceId(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/http-log")
                        .header("X-Trace-Id", "trace-http-log-rejected-gateway")
                        .header("X-User-Id", "invalid-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(completionLog(output, "trace-http-log-rejected-gateway"))
                .contains("| userId  : -\n")
                .contains("| status  : 400\n");
    }

    @Test
    void shouldExcludeHealthEndpoint(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Trace-Id", "trace-health-excluded"))
                .andExpect(status().isOk());

        assertThat(output.getOut()).doesNotContain("| traceId : trace-health-excluded");
    }

    private String completionLog(CapturedOutput output, String traceId) {
        String traceLine = "| traceId : " + traceId + "\n";
        String loggedOutput = output.getOut();
        assertThat(loggedOutput).containsOnlyOnce(traceLine);
        int start = loggedOutput.indexOf(traceLine);
        int end = loggedOutput.indexOf("+----------------------------------------", start);
        assertThat(end).isGreaterThan(start);
        return loggedOutput.substring(start, end);
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
