package com.oigit.admin.boot.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFileStorageConfigurationTests {

    @Test
    void applicationDefaultsFileStorageToLocal() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));

        assertThat(yaml.getObject())
            .containsEntry("platform.file.storage.type", "${FILE_STORAGE_TYPE:local}");
    }

    @Test
    void applicationDoesNotActivateDevelopmentProfileByDefault() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));

        assertThat(yaml.getObject())
            .doesNotContainKey("spring.profiles.active");
    }

    @Test
    void applicationUsesMicrosecondLogicDeleteValue() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));

        assertThat(yaml.getObject())
            .containsEntry(
                "mybatis-plus.global-config.db-config.logic-delete-value",
                "CAST(UNIX_TIMESTAMP(NOW(6)) * 1000000 AS UNSIGNED)"
            );
    }

    @Test
    void developmentProfileRequiresExplicitJwtSecret() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-dev.yml"));

        assertThat(yaml.getObject())
            .containsEntry("platform.iam.jwt-secret", "${IAM_JWT_SECRET}");
    }
}
