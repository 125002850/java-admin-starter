package com.oigit.admin.staff.infra.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.appcik.CIConfig;
import com.oigit.appcik.core.StaffContext;
import com.oigit.appcik.core.service.sso.SsoProperties;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.exception.CommonErrorCode;
import com.oigit.admin.core.exception.ErrorCode;
import com.oigit.admin.core.http.OutboundHttpClientOptions;
import com.oigit.admin.core.http.OutboundHttpExecutor;
import com.oigit.admin.core.http.OutboundHttpResponse;
import com.oigit.admin.staff.enums.StaffErrorCode;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import javax.net.ssl.SSLException;
import okhttp3.Request;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class CikStaffPasswordProviderTests {

    private final OutboundHttpExecutor http = mock(OutboundHttpExecutor.class);
    private final CIConfig config = new CIConfig();
    private final CikStaffPasswordProvider provider = createProvider();

    private CikStaffPasswordProvider createProvider() {
        SsoProperties sso = new SsoProperties();
        sso.setServerAddr("http://sso.test");
        sso.setBasePath("/gateway");
        config.setSso(sso);
        return new CikStaffPasswordProvider(config, StaffContext::new, http, new ObjectMapper());
    }

    @Test
    void shouldUseSdkContractWithBoundedNonRetryingHttpCall() throws Exception {
        respond(200, "{\"success\":true,\"code\":200}");
        change();
        var options = ArgumentCaptor.forClass(OutboundHttpClientOptions.class);
        var request = ArgumentCaptor.forClass(Request.class);
        var limit = ArgumentCaptor.forClass(Integer.class);
        verify(http).execute(options.capture(), request.capture(), limit.capture());
        assertThat(options.getValue().connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(options.getValue().readTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(options.getValue().callTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(options.getValue().retryOnConnectionFailure()).isFalse();
        assertThat(options.getValue().followRedirects()).isFalse();
        assertThat(limit.getValue()).isEqualTo(65536);
        assertThat(request.getValue().url().toString()).isEqualTo("http://sso.test/gateway/entUser/updatePassword");
        assertThat(request.getValue().method()).isEqualTo("POST");
        Buffer buffer = new Buffer();
        request.getValue().body().writeTo(buffer);
        var body = new ObjectMapper().readTree(buffer.readUtf8());
        assertThat(body.path("account").asText()).isEqualTo("TEST-STAFF-12");
        assertThat(body.path("oldPassword").asText()).isEqualTo("old-secret");
        assertThat(body.path("newPassword").asText()).isEqualTo("new-secret");
        assertThat(body.path("repeatPassword").asText()).isEqualTo("new-secret");
    }

    @ParameterizedTest
    @ValueSource(ints = { 200, 400, 422 })
    void shouldPreserveBusinessRejectionsAndHideUnknownMessages(int status) throws Exception {
        respond(status, "{\"success\":false,\"message\":\"旧密码错误\"}");
        assertRejected(StaffErrorCode.STAFF_PASSWORD_CURRENT_INVALID);
        respond(status, "{\"success\":false,\"message\":\"upstream new-secret\"}");
        assertRejected(StaffErrorCode.STAFF_PASSWORD_REJECTED);
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 422 })
    void shouldPreserveSdkCompatibilityForLegacyRejectionResponses(int status) throws Exception {
        respond(status, "{\"message\":\"旧密码错误\"}");
        assertRejected(StaffErrorCode.STAFF_PASSWORD_CURRENT_INVALID);
        respond(status, "{\"code\":400}");
        assertRejected(StaffErrorCode.STAFF_PASSWORD_REJECTED);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "null", "{}", "[]", "<html>new-secret</html>",
        "{\"success\":", "{\"code\":200}", "{\"success\":null}", "{\"success\":{}}" })
    void shouldDistinguishInvalidResponseFromTransportFailure(String body) throws Exception {
        respond(200, body);
        assertRejected(StaffErrorCode.STAFF_PASSWORD_RESPONSE_INVALID);
        verify(http).execute(any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 422 })
    void shouldRejectContradictoryOrEmptyBusinessResponses(int status) throws Exception {
        respond(status, "{\"success\":true}");
        assertRejected(StaffErrorCode.STAFF_PASSWORD_RESPONSE_INVALID);
        respond(status, "{}");
        assertRejected(StaffErrorCode.STAFF_PASSWORD_RESPONSE_INVALID);
    }

    @ParameterizedTest
    @ValueSource(ints = { 201, 204, 302, 401, 403, 404, 429, 500, 502, 503, 504 })
    void shouldClassifyHttpFailuresBeforeParsingBody(int status) throws Exception {
        respond(status, "<html>new-secret</html>");
        assertRejected(StaffErrorCode.STAFF_PASSWORD_HTTP_ERROR);
        // HTTP 故障不能因响应声称成功或包含旧密码错误而被覆盖。
        respond(status, "{\"success\":true}");
        assertRejected(StaffErrorCode.STAFF_PASSWORD_HTTP_ERROR);
        respond(status, "{\"success\":false,\"message\":\"旧密码错误\"}");
        assertRejected(StaffErrorCode.STAFF_PASSWORD_HTTP_ERROR);
    }

    @Test
    void shouldReportOversizedResponseWithoutParsingOrRetrying() throws Exception {
        when(http.execute(any(), any(), anyInt()))
                .thenReturn(new OutboundHttpResponse(200, new byte[0], true, false, null, Map.of()));
        assertRejected(StaffErrorCode.STAFF_PASSWORD_RESPONSE_TOO_LARGE);
        verify(http).execute(any(), any(), anyInt());
    }

    @Test
    void shouldReportUnsupportedEncodingWithoutParsingOrRetrying() throws Exception {
        when(http.execute(any(), any(), anyInt()))
                .thenReturn(new OutboundHttpResponse(200, new byte[0], false, true, null, Map.of()));
        assertRejected(StaffErrorCode.STAFF_PASSWORD_RESPONSE_ENCODING_UNSUPPORTED);
        verify(http).execute(any(), any(), anyInt());
    }

    @Test
    void shouldRejectMissingSsoConfigurationBeforeMakingRequest() {
        config.setSso(null);
        assertRejected(StaffErrorCode.STAFF_PASSWORD_CONFIG_MISSING);
        verifyNoInteractions(http);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t" })
    void shouldRejectMissingServerBeforeMakingRequest(String server) {
        config.getSso().setServerAddr(server);
        assertRejected(StaffErrorCode.STAFF_PASSWORD_CONFIG_MISSING);
        verifyNoInteractions(http);
    }

    @ParameterizedTest
    @ValueSource(strings = { "sso.test", "ftp://sso.test", "http://", "http://[invalid", "http://sso.test:invalid" })
    void shouldRejectInvalidServerBeforeMakingRequest(String server) {
        config.getSso().setServerAddr(server);
        assertRejected(StaffErrorCode.STAFF_PASSWORD_CONFIG_INVALID);
        verifyNoInteractions(http);
    }

    @ParameterizedTest
    @MethodSource("transportFailures")
    void shouldClassifyTransportFailuresWithoutLeakingDetailsOrRetrying(IOException exception, StaffErrorCode error)
            throws Exception {
        when(http.execute(any(), any(), anyInt())).thenThrow(exception);
        assertRejected(error);
        verify(http).execute(any(), any(), anyInt());
    }

    private static Stream<Arguments> transportFailures() {
        String sensitiveMessage = "new-secret upstream response";
        return Stream.of(
                Arguments.of(new IOException(sensitiveMessage), StaffErrorCode.STAFF_PASSWORD_CALL_FAILED),
                Arguments.of(new ConnectException(sensitiveMessage), StaffErrorCode.STAFF_PASSWORD_CALL_FAILED),
                Arguments.of(new UnknownHostException(sensitiveMessage), StaffErrorCode.STAFF_PASSWORD_CALL_FAILED),
                Arguments.of(new SSLException(sensitiveMessage), StaffErrorCode.STAFF_PASSWORD_CALL_FAILED),
                Arguments.of(new SocketTimeoutException(sensitiveMessage), StaffErrorCode.STAFF_PASSWORD_CALL_TIMEOUT),
                Arguments.of(new InterruptedIOException(sensitiveMessage), StaffErrorCode.STAFF_PASSWORD_CALL_TIMEOUT));
    }

    @Test
    void shouldPreserveThreadInterruptionWithoutMisreportingTimeout() throws Exception {
        when(http.execute(any(), any(), anyInt())).thenThrow(new InterruptedIOException("new-secret"));
        Thread.currentThread().interrupt();
        try {
            assertRejected(StaffErrorCode.STAFF_PASSWORD_CALL_FAILED);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldHideUnexpectedSdkFailuresAndNotRetry() throws Exception {
        when(http.execute(any(), any(), anyInt())).thenThrow(new IllegalStateException("new-secret upstream response"));
        assertRejected(StaffErrorCode.STAFF_PASSWORD_CALL_FAILED);
        verify(http).execute(any(), any(), anyInt());
    }

    @Test
    void shouldLogDiagnosticMetadataWithoutSensitiveValuesOrExceptionCauses() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(CikStaffPasswordProvider.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        String previousTraceId = MDC.get("traceId");
        MDC.put("traceId", "password-change-test");
        try {
            respond(200, "<html>old-secret new-secret upstream</html>");
            assertRejected(StaffErrorCode.STAFF_PASSWORD_RESPONSE_INVALID);
            when(http.execute(any(), any(), anyInt())).thenThrow(new IOException("old-secret new-secret upstream"));
            assertRejected(StaffErrorCode.STAFF_PASSWORD_CALL_FAILED);

            assertThat(appender.list).hasSize(2).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("costMs=", "exceptionType=")
                        .doesNotContain("old-secret", "new-secret", "TEST-STAFF-12", "<html>", "sso.test");
                assertThat(event.getThrowableProxy()).isNull();
                assertThat(event.getMDCPropertyMap()).containsEntry("traceId", "password-change-test");
            });
            assertThat(appender.list.get(0).getFormattedMessage())
                    .contains("code=3004016", "reason=invalid_json", "upstreamStatus=200");
            assertThat(appender.list.get(1).getFormattedMessage())
                    .contains("code=3004013", "reason=transport_error", "exceptionType=IOException");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            if (previousTraceId == null) {
                MDC.remove("traceId");
            } else {
                MDC.put("traceId", previousTraceId);
            }
        }
    }

    private void respond(int status, String body) throws Exception {
        when(http.execute(any(), any(), anyInt())).thenReturn(new OutboundHttpResponse(
                status, body.getBytes(StandardCharsets.UTF_8), false, false, null, Map.of()));
    }

    private void change() {
        provider.change("TEST-STAFF-12", "old-secret", "new-secret", "new-secret");
    }

    private void assertRejected(ErrorCode code) {
        assertThatThrownBy(this::change).isInstanceOfSatisfying(BizException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(code);
            assertThat(exception.getMessage()).doesNotContain("new-secret", "upstream");
            assertThat(exception.getCause()).isNull();
        });
    }
}
