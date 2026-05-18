package com.demo.core.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantFilterTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void should_delegate_tenant_resolution_and_clear_context_after_request() throws Exception {
        TenantResolver tenantResolver = mock(TenantResolver.class);
        FilterChain filterChain = mock(FilterChain.class);
        TenantFilter filter = new TenantFilter(tenantResolver, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tenantResolver.resolve(any())).thenReturn(Optional.of(100L));

        filter.doFilter(request, response, filterChain);

        verify(tenantResolver).resolve(any());
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void should_serialize_error_response_when_invalid_tenant_message_contains_quotes() throws Exception {
        TenantResolver tenantResolver = mock(TenantResolver.class);
        FilterChain filterChain = mock(FilterChain.class);
        TenantFilter filter = new TenantFilter(tenantResolver, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        InvalidTenantHeaderException exception = new InvalidTenantHeaderException("ignored") {
            @Override
            public String getMessage() {
                return "bad \"tenant\" value";
            }
        };

        when(tenantResolver.resolve(any())).thenThrow(exception);

        filter.doFilter(request, response, filterChain);

        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(jsonNode.get("code").asInt()).isEqualTo(400);
        assertThat(jsonNode.get("msg").asText()).isEqualTo("bad \"tenant\" value");
        assertThat(jsonNode.get("data").isNull()).isTrue();
    }
}
