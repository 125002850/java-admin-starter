package com.demo.core.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class TenantFilterTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void should_delegate_tenant_resolution_and_clear_context_after_request() throws Exception {
        AtomicBoolean resolverCalled = new AtomicBoolean(false);
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        TenantResolver tenantResolver = request -> {
            resolverCalled.set(true);
            return Optional.of(100L);
        };
        FilterChain filterChain = (req, res) -> {
            chainCalled.set(true);
            assertThat(TenantContext.getTenantId()).isEqualTo(100L);
        };
        TenantFilter filter = new TenantFilter(tenantResolver, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(resolverCalled.get()).isTrue();
        assertThat(chainCalled.get()).isTrue();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void should_serialize_error_response_when_invalid_tenant_message_contains_quotes() throws Exception {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        TenantResolver tenantResolver = request -> {
            throw new InvalidTenantHeaderException("ignored") {
                @Override
                public String getMessage() {
                    return "bad \"tenant\" value";
                }
            };
        };
        FilterChain filterChain = (req, res) -> chainCalled.set(true);
        TenantFilter filter = new TenantFilter(tenantResolver, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(chainCalled.get()).isFalse();
        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(jsonNode.get("code").asInt()).isEqualTo(400);
        assertThat(jsonNode.get("msg").asText()).isEqualTo("bad \"tenant\" value");
        assertThat(jsonNode.get("data").isNull()).isTrue();
    }
}
