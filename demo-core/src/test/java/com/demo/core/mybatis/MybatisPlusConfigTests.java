package com.demo.core.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusConfigTests {

    private final MybatisPlusConfig config = new MybatisPlusConfig();

    @Test
    void should_register_pagination_interceptor() {
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

        assertThat(interceptor.getInterceptors())
            .hasSize(2)
            .element(0).isInstanceOf(PaginationInnerInterceptor.class);
    }

    @Test
    void should_register_optimistic_locker_interceptor() {
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

        assertThat(interceptor.getInterceptors())
            .element(1).isInstanceOf(OptimisticLockerInnerInterceptor.class);
    }

    @Test
    void should_not_define_manual_global_config_bean() {
        Method[] methods = MybatisPlusConfig.class.getDeclaredMethods();

        assertThat(Arrays.stream(methods).map(Method::getName))
            .doesNotContain("globalConfig");
    }
}
