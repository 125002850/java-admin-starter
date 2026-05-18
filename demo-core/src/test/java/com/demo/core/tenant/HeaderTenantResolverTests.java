package com.demo.core.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderTenantResolverTests {

    private final HeaderTenantResolver resolver = new HeaderTenantResolver();

    @Test
    void should_return_empty_when_header_missing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(resolver.resolve(request)).isEmpty();
    }

    @Test
    void should_parse_tenant_id_from_header() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantResolver.TENANT_ID_HEADER, "123");

        assertThat(resolver.resolve(request)).contains(123L);
    }

    @Test
    void should_fail_when_header_is_not_numeric() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantResolver.TENANT_ID_HEADER, "tenant-a");

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(InvalidTenantHeaderException.class)
                .hasMessage("X-Tenant-Id 值非法: tenant-a");
    }
}
