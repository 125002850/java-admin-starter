package com.oigit.admin.boot.mybatis;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.oigit.admin.boot.AdminBootApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AdminBootApplication.class)
@ActiveProfiles("test")
class LogicalDeleteConfigurationTests {

    @Autowired
    private MybatisPlusProperties mybatisPlusProperties;

    @Test
    void test_profile_should_use_numeric_timestamp_logic_delete_value() {
        MybatisPlusProperties.CoreConfiguration configuration = mybatisPlusProperties.getConfiguration();

        assertThat(mybatisPlusProperties.getGlobalConfig()).isNotNull();
        assertThat(mybatisPlusProperties.getGlobalConfig().getDbConfig()).isNotNull();
        assertThat(mybatisPlusProperties.getGlobalConfig().getDbConfig().getLogicDeleteField()).isEqualTo("deleted");
        assertThat(mybatisPlusProperties.getGlobalConfig().getDbConfig().getLogicDeleteValue()).isEqualTo("unix_timestamp()");
        assertThat(mybatisPlusProperties.getGlobalConfig().getDbConfig().getLogicNotDeleteValue()).isEqualTo("0");
        assertThat(configuration).isNotNull();
    }
}
