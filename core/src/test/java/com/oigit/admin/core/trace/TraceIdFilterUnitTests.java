package com.oigit.admin.core.trace;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterUnitTests {

    @Test
    void shouldReplaceUnsafeTraceIdInsteadOfWritingItToMdcOrResponse() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "unsafe trace/id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdDuringRequest = new AtomicReference<>();

        filter.doFilter(request, response,
                (req, res) -> traceIdDuringRequest.set(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)));

        assertThat(traceIdDuringRequest.get())
                .isNotEqualTo("unsafe trace/id")
                .matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(traceIdDuringRequest.get());
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }
}
