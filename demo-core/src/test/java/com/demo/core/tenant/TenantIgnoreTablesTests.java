package com.demo.core.tenant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIgnoreTablesTests {

    @Test
    void should_mark_platform_tables_as_ignored() {
        assertThat(TenantIgnoreTables.contains("sys_tenant")).isTrue();
        assertThat(TenantIgnoreTables.contains("sys_tenant_global")).isTrue();
        assertThat(TenantIgnoreTables.contains("sys_dict_type_global")).isTrue();
        assertThat(TenantIgnoreTables.contains("SYS_TENANT_GLOBAL")).isTrue();
        assertThat(TenantIgnoreTables.contains("sys_user")).isFalse();
        assertThat(TenantIgnoreTables.contains(" ")).isFalse();
    }
}
