package com.oigit.admin.core.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.logging.HttpLoggingProperties;
import com.oigit.admin.core.operator.OperatorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static com.oigit.admin.core.audit.OperationAuditRequestAttributes.METADATA;
import static com.oigit.admin.core.audit.OperationAuditRequestAttributes.RESULT_CODE;
import static com.oigit.admin.core.audit.OperationAuditRequestAttributes.RESULT_MESSAGE;
import static com.oigit.admin.core.trace.TraceIdFilter.TRACE_ID_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OperationAuditFilterTests {

    private final OperationAuditWriter writer = mock(OperationAuditWriter.class);
    private final OperationAuditFilter filter = new OperationAuditFilter(
            writer, new ObjectMapper(), new HttpLoggingProperties()
    );

    @AfterEach
    void clearContext() {
        OperatorContext.clear();
        MDC.clear();
    }

    @Test
    void should_capture_success_with_masked_request_and_operator_snapshot() throws Exception {
        MockHttpServletRequest request = auditedJsonRequest("""
                {"name":"测试","password":"secret","nested":{"mobile":"13800138000"}}
                """);
        request.setRemoteAddr("10.0.0.9");
        request.setAttribute(OperatorContext.REQUEST_ATTRIBUTE_OPERATOR_ID, 8L);
        request.setAttribute("audit.operator.username", "stone");
        request.setAttribute("audit.operator.realName", "石头");
        request.addHeader("X-Forwarded-For", "120.229.36.171, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OperatorContext.set(8L, "stone", "13800138000", "石头");
        MDC.put(TRACE_ID_MDC_KEY, "trace-001");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletRequest.getInputStream().readAllBytes();
            servletRequest.setAttribute(RESULT_CODE, 200);
            servletRequest.setAttribute(RESULT_MESSAGE, "成功");
            ((MockHttpServletResponse) servletResponse).setStatus(200);
        });

        OperationAuditCommand command = capturedCommand();
        assertThat(command.resultStatus()).isEqualTo(OperationAuditResultStatus.SUCCESS);
        assertThat(command.operatorId()).isEqualTo(8L);
        assertThat(command.operatorUsername()).isEqualTo("stone");
        assertThat(command.operatorRealName()).isEqualTo("石头");
        assertThat(command.clientIp()).isEqualTo("10.0.0.9");
        assertThat(command.traceId()).isEqualTo("trace-001");
        assertThat(command.requestParams()).contains("\"name\":\"测试\"")
                .contains("\"password\":\"***\"")
                .contains("\"mobile\":\"***\"")
                .doesNotContain("13800138000", "secret");
        assertThat(command.errorMessage()).isNull();
    }

    @Test
    void should_capture_business_and_http_failures_without_response_body() throws Exception {
        MockHttpServletRequest request = auditedJsonRequest("{\"id\":1}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletRequest.getInputStream().readAllBytes();
            servletRequest.setAttribute(RESULT_CODE, 3006001);
            servletRequest.setAttribute(RESULT_MESSAGE, "操作日志不存在");
            ((MockHttpServletResponse) servletResponse).setStatus(422);
            servletResponse.getWriter().write("response body must not be captured");
        });

        OperationAuditCommand command = capturedCommand();
        assertThat(command.resultStatus()).isEqualTo(OperationAuditResultStatus.FAILED);
        assertThat(command.httpStatus()).isEqualTo(422);
        assertThat(command.resultCode()).isEqualTo(3006001);
        assertThat(command.errorMessage()).isEqualTo("操作日志不存在");
        assertThat(command.requestParams()).doesNotContain("response body must not be captured");
    }

    @Test
    void should_omit_oversized_and_multipart_payloads() throws Exception {
        MockHttpServletRequest oversized = auditedJsonRequest("{\"payload\":\"" + "a".repeat(9000) + "\"}");
        filter.doFilter(oversized, new MockHttpServletResponse(), (request, response) -> {
            request.getInputStream().readAllBytes();
            request.setAttribute(RESULT_CODE, 200);
        });
        assertThat(capturedCommand().requestParams()).isEqualTo("<omitted: body exceeds 8192 bytes>");

        OperationAuditWriter multipartWriter = mock(OperationAuditWriter.class);
        OperationAuditFilter multipartFilter = new OperationAuditFilter(
                multipartWriter, new ObjectMapper(), new HttpLoggingProperties()
        );
        MockHttpServletRequest multipart = auditedJsonRequest("");
        multipart.setContentType("multipart/form-data; boundary=test");
        multipartFilter.doFilter(multipart, new MockHttpServletResponse(), (request, response) ->
                request.setAttribute(RESULT_CODE, 200));
        ArgumentCaptor<OperationAuditCommand> captor = ArgumentCaptor.forClass(OperationAuditCommand.class);
        verify(multipartWriter).write(captor.capture());
        assertThat(captor.getValue().requestParams()).isEqualTo("-");
    }

    @Test
    void audit_write_failure_should_not_change_business_response() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(writer)
                .write(org.mockito.ArgumentMatchers.any(OperationAuditCommand.class));
        MockHttpServletRequest request = auditedJsonRequest("{\"id\":1}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatCode(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletRequest.getInputStream().readAllBytes();
            servletRequest.setAttribute(RESULT_CODE, 200);
            servletResponse.getWriter().write("business-ok");
        })).doesNotThrowAnyException();

        assertThat(response.getContentAsString()).isEqualTo("business-ok");
    }

    private MockHttpServletRequest auditedJsonRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test/create");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setAttribute(METADATA, new OperationAuditMetadata(
                "test.module", "测试模块", OperationAuditAction.CREATE, "新增测试"
        ));
        return request;
    }

    private OperationAuditCommand capturedCommand() {
        ArgumentCaptor<OperationAuditCommand> captor = ArgumentCaptor.forClass(OperationAuditCommand.class);
        verify(writer).write(captor.capture());
        return captor.getValue();
    }
}
