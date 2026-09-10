package com.oigit.admin.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationGuardTests {

    private static final String PACKAGED_APPLICATION_SOURCE =
            "Config resource 'class path resource [application.yml]' via location 'classpath:/'";
    private static final String PACKAGED_DOCKER_APPLICATION_SOURCE =
            "Config resource 'file [/app/application.yml]' via location 'application.yml'";

    @Test
    void ignoresPackagedDevelopmentDefaultsOutsideProduction() {
        StandardEnvironment environment = environmentWithValidExternalOverrides();
        environment.getPropertySources().addFirst(new MapPropertySource(
                PACKAGED_APPLICATION_SOURCE,
                Map.of("spring.datasource.password", "development-password")));

        assertThatCode(() -> ProductionConfigurationGuard.verify(environment)).doesNotThrowAnyException();
    }

    @Test
    void rejectsCombiningProductionAndDevelopmentProfiles() {
        StandardEnvironment environment = environmentWithValidExternalOverrides();
        environment.setActiveProfiles("production", "dev");

        assertThatThrownBy(() -> ProductionConfigurationGuard.verify(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dev")
                .hasMessageNotContaining("external-password");
    }

    @Test
    void rejectsPackagedSensitiveDefaultsWithoutLoggingTheirValues() {
        StandardEnvironment environment = environmentWithValidExternalOverrides();
        environment.setActiveProfiles("production");
        environment.getPropertySources().addFirst(new MapPropertySource(
                PACKAGED_APPLICATION_SOURCE,
                Map.of("spring.datasource.password", "development-password")));

        assertThatThrownBy(() -> ProductionConfigurationGuard.verify(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.password")
                .hasMessageContaining("raw values are omitted")
                .hasMessageNotContaining("development-password")
                .hasMessageNotContaining("external-password");
    }

    @Test
    void rejectsTheApplicationFileBakedIntoTheDockerImage() {
        StandardEnvironment environment = environmentWithValidExternalOverrides();
        environment.setActiveProfiles("production");
        environment.getPropertySources().addFirst(new MapPropertySource(
                PACKAGED_DOCKER_APPLICATION_SOURCE,
                Map.of("spring.datasource.password", "docker-development-password")));

        assertThatThrownBy(() -> ProductionConfigurationGuard.verify(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.password")
                .hasMessageNotContaining("docker-development-password");
    }

    @Test
    void acceptsExternalOverridesForEveryProtectedProperty() {
        StandardEnvironment environment = environmentWithValidExternalOverrides();
        environment.setActiveProfiles("prod");

        assertThatCode(() -> ProductionConfigurationGuard.verify(environment)).doesNotThrowAnyException();
    }

    @Test
    void acceptsAnExplicitEnvironmentVariableUsedByAPackagedPlaceholder() {
        StandardEnvironment environment = environmentWithValidExternalOverrides();
        environment.setActiveProfiles("production");
        environment.getPropertySources().addFirst(new MapPropertySource(
                PACKAGED_APPLICATION_SOURCE,
                Map.of("spring.datasource.password",
                        "${JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD:development-password}")));
        environment.getPropertySources().addFirst(new MapPropertySource(
                "systemEnvironment",
                Map.of("JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD", "external-password")));

        assertThatCode(() -> ProductionConfigurationGuard.verify(environment)).doesNotThrowAnyException();
    }

    @Test
    void springFactoriesRegistrationRejectsAnUnsafeProductionBootstrap() {
        SpringApplication application = productionApplication();
        application.setDefaultProperties(Map.of(
                "spring.application.name", "java-admin-starter"));

        assertThatThrownBy(application::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing external overrides")
                .hasMessageContaining("raw values are omitted");
    }

    @Test
    void springFactoriesRegistrationAllowsASafeProductionBootstrap() {
        SpringApplication application = productionApplication();
        application.setEnvironment(environmentWithValidExternalOverrides());

        assertThatCode(() -> {
            try (ConfigurableApplicationContext ignored = application.run()) {
                // Context startup proves that spring.factories loaded and ran the guard.
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankExternalValuesAndPackagedPlaceholderFallbacks() {
        for (String value : new String[]{"", "   ", "${MISSING_PRODUCTION_PASSWORD}"}) {
            StandardEnvironment environment = environmentWithValidExternalOverrides();
            environment.setActiveProfiles("prod");
            environment.getPropertySources().addFirst(new MapPropertySource("external", Map.of("spring.datasource.password", value)));
            assertThatThrownBy(() -> ProductionConfigurationGuard.verify(environment))
                    .hasMessageContaining("spring.datasource.password").hasMessageNotContaining(value.isBlank() ? "external-password" : value);
        }
        StandardEnvironment environment = environmentWithValidExternalOverrides();
        environment.setActiveProfiles("prod");
        environment.getPropertySources().addFirst(new MapPropertySource(PACKAGED_APPLICATION_SOURCE,
                Map.of("spring.datasource.password", "${BUNDLED_PASSWORD:default-secret}", "BUNDLED_PASSWORD", "bundled-secret")));
        assertThatThrownBy(() -> ProductionConfigurationGuard.verify(environment))
                .hasMessageContaining("spring.datasource.password").hasMessageNotContaining("bundled-secret");
    }

    private static StandardEnvironment environmentWithValidExternalOverrides() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "external production configuration",
                externalOverrides()));
        return environment;
    }

    private static Map<String, Object> externalOverrides() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.application.name", "java-admin-starter");
        properties.put("spring.datasource.url", "jdbc:mysql://production.example.invalid/app");
        properties.put("spring.datasource.username", "external-user");
        properties.put("spring.datasource.password", "external-password");
        return properties;
    }

    private static SpringApplication productionApplication() {
        SpringApplication application = new SpringApplication(EmptyConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setAdditionalProfiles("production");
        application.setLogStartupInfo(false);
        return application;
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfiguration {
    }
}
