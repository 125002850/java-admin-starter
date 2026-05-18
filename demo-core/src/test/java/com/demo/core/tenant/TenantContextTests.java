package com.demo.core.tenant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTests {

    @Test
    void should_get_same_tenant_after_set_and_return_null_after_clear() {
        TenantContext.setTenantId(100L);
        assertThat(TenantContext.getTenantId()).isEqualTo(100L);
        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void should_require_tenant_context_explicitly() {
        TenantContext.clear();
        assertThatThrownBy(TenantContext::requireTenantId)
            .isInstanceOf(MissingTenantContextException.class)
            .hasMessage("Missing tenant context for tenant-isolated query");
    }
}
