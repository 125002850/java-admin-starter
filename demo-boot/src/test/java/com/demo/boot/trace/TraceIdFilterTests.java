package com.demo.boot.trace;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.demo.core.trace.TraceIdFilter.TRACE_ID_HEADER;
import static com.demo.core.trace.TraceIdFilter.TRACE_ID_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TraceIdFilterTests.TraceIdTestConfig.class)
class TraceIdFilterTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeTraceIdInMdcDuringRequestHandling() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/test/trace-id").header(TRACE_ID_HEADER, "trace-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(TRACE_ID_HEADER, "trace-123"))
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("trace-123");
        assertThat(MDC.get(TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldEchoTraceIdHeaderForHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health").header(TRACE_ID_HEADER, "trace-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(TRACE_ID_HEADER, "trace-123"));

        assertThat(MDC.get(TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateTraceIdAndWriteItBackWhenHeaderMissing() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/test/trace-id"))
                .andExpect(status().isOk())
                .andExpect(header().exists(TRACE_ID_HEADER))
                .andReturn();

        String traceId = mvcResult.getResponse().getHeader(TRACE_ID_HEADER);

        assertThat(traceId).isNotBlank();
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(traceId);
        assertThat(MDC.get(TRACE_ID_MDC_KEY)).isNull();
    }

    @TestConfiguration
    static class TraceIdTestConfig {

        @Bean
        TraceIdEchoController traceIdEchoController() {
            return new TraceIdEchoController();
        }
    }

    @RestController
    static class TraceIdEchoController {

        @GetMapping("/test/trace-id")
        String traceId() {
            return MDC.get(TRACE_ID_MDC_KEY);
        }
    }
}
