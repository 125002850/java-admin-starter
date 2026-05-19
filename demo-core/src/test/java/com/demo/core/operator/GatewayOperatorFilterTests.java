package com.demo.core.operator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOperatorFilterTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        OperatorContext.clear();
    }

    @Test
    void should_set_operator_context_from_headers_and_clear_after_request() throws Exception {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> {
            chainCalled.set(true);
            assertThat(OperatorContext.getOperatorId()).isEqualTo(100L);
            assertThat(OperatorContext.getOperatorName()).isEqualTo("test-user");
        };
        GatewayOperatorFilter filter = new GatewayOperatorFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "100");
        request.addHeader("X-User-Name", "test-user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(OperatorContext.getOperatorId()).isNull();
        assertThat(OperatorContext.getOperatorName()).isNull();
    }

    @Test
    void should_allow_request_when_headers_are_missing() throws Exception {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> chainCalled.set(true);
        GatewayOperatorFilter filter = new GatewayOperatorFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(OperatorContext.getOperatorId()).isNull();
    }

    @Test
    void should_return_400_when_user_id_is_not_numeric() throws Exception {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> chainCalled.set(true);
        GatewayOperatorFilter filter = new GatewayOperatorFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-number");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(chainCalled.get()).isFalse();
        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(jsonNode.get("code").asInt()).isEqualTo(400);
        assertThat(jsonNode.get("msg").asText()).isEqualTo("X-User-Id 值非法: not-a-number");
    }
}
