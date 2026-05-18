package com.demo.core.mybatis;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.demo.core.tenant.MissingTenantContextException;
import com.demo.core.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MybatisPlusConfigTests {

    private final MybatisPlusConfig config = new MybatisPlusConfig();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void should_register_tenant_interceptor_before_pagination() {
        TenantLineHandler tenantLineHandler = config.tenantLineHandler();
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor(tenantLineHandler);

        assertThat(interceptor.getInterceptors())
            .hasSize(2)
            .element(0).isInstanceOf(TenantLineInnerInterceptor.class);
        assertThat(interceptor.getInterceptors())
            .element(1).isInstanceOf(PaginationInnerInterceptor.class);
    }

    @Test
    void should_configure_logic_delete_values() {
        GlobalConfig.DbConfig dbConfig = config.globalConfig().getDbConfig();

        assertThat(dbConfig.getLogicDeleteField()).isEqualTo("deleted");
        assertThat(dbConfig.getLogicDeleteValue()).isEqualTo("1");
        assertThat(dbConfig.getLogicNotDeleteValue()).isEqualTo("0");
    }

    @Test
    void should_fail_fast_when_tenant_context_is_missing() {
        TenantLineHandler tenantLineHandler = config.tenantLineHandler();

        assertThatThrownBy(tenantLineHandler::getTenantId)
            .isInstanceOf(MissingTenantContextException.class)
            .hasMessage("Missing tenant context for tenant-isolated query");
    }

    @Test
    void should_return_current_tenant_id_expression() {
        TenantContext.setTenantId(123L);
        TenantLineHandler tenantLineHandler = config.tenantLineHandler();

        assertThat(tenantLineHandler.getTenantId().toString()).isEqualTo("123");
    }
}
