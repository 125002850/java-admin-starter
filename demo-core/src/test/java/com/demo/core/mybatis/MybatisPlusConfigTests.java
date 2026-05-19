package com.demo.core.mybatis;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusConfigTests {

    private final MybatisPlusConfig config = new MybatisPlusConfig();

    @Test
    void should_register_pagination_interceptor() {
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

        assertThat(interceptor.getInterceptors())
            .hasSize(1)
            .element(0).isInstanceOf(PaginationInnerInterceptor.class);
    }

    @Test
    void should_configure_logic_delete_values() {
        GlobalConfig.DbConfig dbConfig = config.globalConfig().getDbConfig();

        assertThat(dbConfig.getLogicDeleteField()).isEqualTo("deleted");
        assertThat(dbConfig.getLogicDeleteValue()).isEqualTo("1");
        assertThat(dbConfig.getLogicNotDeleteValue()).isEqualTo("0");
    }
}
