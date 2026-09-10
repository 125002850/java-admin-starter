package com.oigit.admin.core.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

final class ProductionConfigurationGuard {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");
    private static final Set<String> DEVELOPMENT_PROFILES = Set.of("dev", "development");
    private static final String CONFIGURATION_PROPERTIES_SOURCE = "configurationProperties";
    private static final Pattern ENVIRONMENT_PLACEHOLDER = Pattern.compile(
            "^\\$\\{([A-Z][A-Z0-9_]*)(?::[^}]*)?}$");
    private static final List<String> SHARED_PROTECTED_PROPERTIES = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password");
    private ProductionConfigurationGuard() {
    }

    static VerificationResult verify(ConfigurableEnvironment environment) {
        Set<String> activeProfiles = normalizedProfiles(environment.getActiveProfiles());
        if (activeProfiles.stream().noneMatch(PRODUCTION_PROFILES::contains)) {
            return VerificationResult.notRequired();
        }
        if (activeProfiles.stream().anyMatch(DEVELOPMENT_PROFILES::contains)) {
            throw new IllegalStateException(
                    "Production configuration verification failed: production and dev profiles "
                            + "cannot be active together; raw values are omitted");
        }

        List<String> requiredProperties = SHARED_PROTECTED_PROPERTIES;
        List<String> unsafeProperties = requiredProperties.stream()
                .filter(propertyName -> !isExternallyProvided(environment, propertyName))
                .toList();
        if (!unsafeProperties.isEmpty()) {
            throw new IllegalStateException(
                    "Production configuration verification failed; missing external overrides for keys="
                            + String.join(",", unsafeProperties)
                            + "; raw values are omitted");
        }
        return VerificationResult.passed(requiredProperties.size());
    }

    private static boolean isExternallyProvided(ConfigurableEnvironment environment,
                                                String propertyName) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (CONFIGURATION_PROPERTIES_SOURCE.equals(propertySource.getName())) {
                continue;
            }
            Object rawValue = propertySource.getProperty(propertyName);
            if (rawValue == null) {
                continue;
            }
            if (!isPackagedDefaultSource(propertySource.getName())) {
                return hasResolvedValue(environment, propertyName);
            }
            return usesExplicitEnvironmentOverride(environment, rawValue);
        }
        return false;
    }

    private static boolean isPackagedDefaultSource(String sourceName) {
        String normalized = sourceName.toLowerCase(Locale.ROOT);
        return normalized.equals("defaultproperties")
                || normalized.contains("class path resource")
                || normalized.contains("classpath:/")
                || normalized.contains("classpath:")
                || normalized.contains("file [/app/application.yml]")
                || normalized.contains("file:application.yml")
                || normalized.contains("file [application.yml]");
    }

    private static boolean usesExplicitEnvironmentOverride(ConfigurableEnvironment environment,
                                                           Object rawValue) {
        Matcher matcher = ENVIRONMENT_PLACEHOLDER.matcher(String.valueOf(rawValue));
        if (!matcher.matches()) {
            return false;
        }
        String variable = matcher.group(1);
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (CONFIGURATION_PROPERTIES_SOURCE.equals(source.getName())) continue;
            if (source.getProperty(variable) != null) {
                return !isPackagedDefaultSource(source.getName()) && hasResolvedValue(environment, variable);
            }
        }
        return false;
    }

    private static boolean hasResolvedValue(ConfigurableEnvironment environment, String key) {
        try {
            return StringUtils.hasText(environment.getProperty(key));
        } catch (IllegalArgumentException ignored) {
            // Placeholder failures must not leak raw configuration values in the startup error.
            return false;
        }
    }

    private static Set<String> normalizedProfiles(String[] profiles) {
        Set<String> normalized = new LinkedHashSet<>();
        Arrays.stream(profiles)
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .forEach(normalized::add);
        return normalized;
    }

    record VerificationResult(boolean required, int protectedPropertyCount) {

        static VerificationResult notRequired() {
            return new VerificationResult(false, 0);
        }

        static VerificationResult passed(int protectedPropertyCount) {
            return new VerificationResult(true, protectedPropertyCount);
        }
    }
}
