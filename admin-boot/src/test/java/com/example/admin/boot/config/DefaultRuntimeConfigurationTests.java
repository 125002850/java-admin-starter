package com.example.admin.boot.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuntimeConfigurationTests {

    @Test
    void applicationDefaultsFileStorageToLocal() {
        assertThat(applicationProperties())
            .containsEntry("platform.file.storage.type", "${FILE_STORAGE_TYPE:local}");
    }

    @Test
    void applicationDoesNotActivateDevelopmentProfileByDefault() {
        assertThat(applicationProperties())
            .doesNotContainKey("spring.profiles.active");
    }

    @Test
    void applicationUsesMicrosecondLogicDeleteValue() {
        assertThat(applicationProperties())
            .containsEntry(
                "mybatis-plus.global-config.db-config.logic-delete-value",
                "CAST(UNIX_TIMESTAMP(NOW(6)) * 1000000 AS UNSIGNED)"
            );
    }

    private java.util.Properties applicationProperties() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        return yaml.getObject();
    }
}
