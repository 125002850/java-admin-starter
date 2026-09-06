package com.oigit.admin.core.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.operator.ClientIpResolver;
import com.oigit.admin.core.operator.GatewayOperatorFilter;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.core.trace.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestLoggingFilterTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    private final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter(
            new HttpLoggingProperties(), objectMapper, beanFactory.getBeanProvider(ClientIpResolver.class));
    private final Logger logger = (Logger) LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void cleanUp() {
        logger.detachAppender(appender);
        appender.stop();
        OperatorContext.clear();
        MDC.clear();
    }

    @Test
    void shouldIgnoreRawUserIdHeaderWithoutAuthenticatedIdentity() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-User-Id", "99999");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(completionLog())
                .contains("| userId  : -\n")
                .doesNotContain("99999");
    }

    @ParameterizedTest
    @CsvSource({
            "X-Forwarded-For, 198.51.100.8",
            "X-Real-IP, 198.51.100.8",
            "Forwarded, for=198.51.100.8"
    })
    void shouldIgnoreForwardedHeadersWithoutClientIpResolver(String header, String value) throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(header, value);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(completionLog())
                .contains("| ip      : 203.0.113.10\n")
                .doesNotContain("198.51.100.8");
    }

    @Test
    void shouldKeepAuthenticatedIdentityAfterContextCleanupDespiteSpoofedHeader() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-User-Id", "99999");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            OperatorContext.set(20002L, "authenticated-user", null);
            req.setAttribute(OperatorContext.REQUEST_ATTRIBUTE_OPERATOR_ID, 20002L);
            try {
                assertThat(OperatorContext.getOperatorId()).isEqualTo(20002L);
            } finally {
                OperatorContext.clear();
            }
        });

        assertThat(OperatorContext.getOperatorId()).isNull();
        assertThat(request.getAttribute(OperatorContext.REQUEST_ATTRIBUTE_OPERATOR_ID)).isEqualTo(20002L);
        assertThat(completionLog())
                .contains("| userId  : 20002\n")
                .doesNotContain("99999");
    }

    @Test
    void shouldNotTreatGatewayHeaderParsingAsAuthentication() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-User-Id", "99999");
        GatewayOperatorFilter gatewayFilter = new GatewayOperatorFilter(objectMapper);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
                gatewayFilter.doFilter(req, res, (innerRequest, innerResponse) -> {
                    assertThat(OperatorContext.getOperatorId()).isEqualTo(99999L);
                    assertThat(innerRequest.getAttribute(OperatorContext.REQUEST_ATTRIBUTE_OPERATOR_ID)).isNull();
                }));

        assertThat(OperatorContext.getOperatorId()).isNull();
        assertThat(completionLog()).contains("| userId  : -\n");
    }

    @Test
    void shouldLogRemoteAddressWhenNoForwardedHeadersArePresent() throws Exception {
        filter.doFilter(request(), new MockHttpServletResponse(), (req, res) -> { });

        assertThat(completionLog()).contains("| ip      : 203.0.113.10\n");
    }

    @Test
    void shouldUseConfiguredClientIpResolver() throws Exception {
        beanFactory.registerSingleton("clientIpResolver", (ClientIpResolver) req -> {
            assertThat(req.getRemoteAddr()).isEqualTo("203.0.113.10");
            return "198.51.100.20";
        });
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "198.51.100.99");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(completionLog())
                .contains("| ip      : 198.51.100.20\n")
                .doesNotContain("198.51.100.99");
    }

    @Test
    void shouldLogGatewayRejectionBeforeTraceContextIsCleared() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-Trace-Id", "trace-gateway-rejected");
        request.addHeader("X-User-Id", "invalid-user");
        MockHttpServletResponse response = new MockHttpServletResponse();
        GatewayOperatorFilter gatewayFilter = new GatewayOperatorFilter(objectMapper);

        new TraceIdFilter().doFilter(request, response, (traceRequest, traceResponse) ->
                filter.doFilter(traceRequest, traceResponse, (logRequest, logResponse) ->
                        gatewayFilter.doFilter(logRequest, logResponse, (req, res) -> {
                            throw new AssertionError("Rejected request must not reach the application");
                        })));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(completionLog())
                .contains("| traceId : trace-gateway-rejected\n")
                .contains("| userId  : -\n")
                .contains("| status  : 400\n");
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/http-log");
        request.setRemoteAddr("203.0.113.10");
        return request;
    }

    private String completionLog() {
        assertThat(appender.list).hasSize(1);
        return appender.list.get(0).getFormattedMessage();
    }
}
