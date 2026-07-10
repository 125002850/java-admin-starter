package com.example.admin.boot.contract;

import com.example.admin.core.exception.CommonErrorCode;
import com.example.admin.core.exception.ErrorCode;
import com.example.admin.core.query.exception.DynamicQueryErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeContractTests {

    @Test
    void contractScanShouldIncludeDynamicQueryErrorCodes() {
        assertThat(scanErrorCodeEnums("com.example.admin")).contains(DynamicQueryErrorCode.class);
    }

    @Test
    void all_error_code_enums_should_have_unique_codes_and_non_blank_msgs() {
        Set<Class<?>> errorCodeEnums = scanErrorCodeEnums("com.example.admin");
        Map<Integer, String> owners = new HashMap<>();

        for (Class<?> errorCodeEnum : errorCodeEnums) {
            for (Object constant : errorCodeEnum.getEnumConstants()) {
                ErrorCode value = (ErrorCode) constant;
                assertThat(value.getMsg()).isNotBlank();
                String previous = owners.putIfAbsent(value.getCode(), errorCodeEnum.getName() + "#" + constant);
                assertThat(previous).as("duplicate error code: " + value.getCode()).isNull();
            }
        }
    }

    @Test
    void business_error_codes_should_not_reuse_common_http_codes() {
        Set<Integer> reserved = Set.of(200, 400, 401, 403, 404, 429, 500);
        Set<Class<?>> errorCodeEnums = scanErrorCodeEnums("com.example.admin");

        for (Class<?> errorCodeEnum : errorCodeEnums) {
            if (errorCodeEnum == CommonErrorCode.class) {
                continue;
            }
            for (Object constant : errorCodeEnum.getEnumConstants()) {
                ErrorCode value = (ErrorCode) constant;
                assertThat(reserved).doesNotContain(value.getCode());
            }
        }
    }

    @Test
    void main_sources_should_not_new_r_or_throw_runtime_exception_for_business_errors() throws Exception {
        List<Path> sources = scanMainSourceFiles();

        for (Path source : sources) {
            if (source.endsWith("admin-core/src/main/java/com/example/admin/core/web/R.java")) {
                continue;
            }
            String content = Files.readString(source);
            assertThat(content).doesNotContain("new R<>(").doesNotContain("new R(");
            assertThat(content).doesNotContain("throw new RuntimeException(");
        }
    }

    private static Set<Class<?>> scanErrorCodeEnums(String basePackage) {
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(ErrorCode.class));

        return provider.findCandidateComponents(basePackage).stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(Objects::nonNull)
                .map(ErrorCodeContractTests::loadClass)
                .filter(Class::isEnum)
                .filter(ErrorCode.class::isAssignableFrom)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("failed to load " + className, ex);
        }
    }

    private static List<Path> scanMainSourceFiles() throws IOException {
        Path repoRoot = resolveRepoRoot();
        List<Path> sourceRoots = List.of(
                repoRoot.resolve("admin-core/src/main/java"),
                repoRoot.resolve("admin-system/src/main/java"),
                repoRoot.resolve("admin-boot/src/main/java"));
        List<Path> sources = new ArrayList<>();

        for (Path sourceRoot : sourceRoots) {
            if (Files.notExists(sourceRoot)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(sourceRoot)) {
                sources.addAll(stream
                        .filter(path -> path.toString().endsWith(".java"))
                        .toList());
            }
        }
        return sources;
    }

    private static Path resolveRepoRoot() throws IOException {
        String mavenRoot = System.getProperty("maven.multiModuleProjectDirectory");
        if (StringUtils.hasText(mavenRoot)) {
            return Path.of(mavenRoot).toAbsolutePath().normalize();
        }

        try {
            Path location = Path.of(ErrorCodeContractTests.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()).toAbsolutePath().normalize();
            for (Path current = location; current != null; current = current.getParent()) {
                if (Files.exists(current.resolve("README.md"))
                        && Files.exists(current.resolve("admin-core/pom.xml"))
                        && Files.exists(current.resolve("admin-boot/pom.xml"))) {
                    return current;
                }
            }
        } catch (URISyntaxException ex) {
            throw new IOException("failed to resolve repository root", ex);
        }

        throw new IOException("failed to locate repository root");
    }
}
