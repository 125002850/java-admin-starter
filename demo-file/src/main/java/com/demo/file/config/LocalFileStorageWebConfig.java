package com.demo.file.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "demo.file.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageWebConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    public LocalFileStorageWebConfig(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseUrl = normalizeBaseUrl(fileStorageProperties.getLocal().getBaseUrl());
        String resourceLocation = ensureTrailingSlash(Path.of(fileStorageProperties.getLocal().getRootDir())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString());

        registry.addResourceHandler(baseUrl + "/**")
                .addResourceLocations(resourceLocation);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "/local-files";
        }
        String normalized = baseUrl.startsWith("/") ? baseUrl : "/" + baseUrl;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String ensureTrailingSlash(String resourceLocation) {
        return resourceLocation.endsWith("/") ? resourceLocation : resourceLocation + "/";
    }
}
