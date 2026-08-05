package com.oigit.admin.core.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "platform.http-logging")
public class HttpLoggingProperties {

    private boolean enabled = true;
    private boolean requestBodyEnabled = true;
    private boolean responseBodyEnabled = true;
    private int maxRequestBodyBytes = 8 * 1024;
    private int maxResponseBodyBytes = 16 * 1024;
    private List<String> excludedPaths = new ArrayList<>(List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/swagger-ui/**",
            "/webjars/**",
            "/local-files/**"
    ));
    private Set<String> sensitiveFields = new LinkedHashSet<>(Set.of(
            "password",
            "passwd",
            "pwd",
            "token",
            "accessToken",
            "refreshToken",
            "idToken",
            "authorization",
            "cookie",
            "setCookie",
            "ticket",
            "secret",
            "secretKey",
            "accessKey",
            "privateKey",
            "phone",
            "mobile",
            "idCard",
            "identityCard",
            "bankCard",
            "cardNo"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequestBodyEnabled() {
        return requestBodyEnabled;
    }

    public void setRequestBodyEnabled(boolean requestBodyEnabled) {
        this.requestBodyEnabled = requestBodyEnabled;
    }

    public boolean isResponseBodyEnabled() {
        return responseBodyEnabled;
    }

    public void setResponseBodyEnabled(boolean responseBodyEnabled) {
        this.responseBodyEnabled = responseBodyEnabled;
    }

    public int getMaxRequestBodyBytes() {
        return maxRequestBodyBytes;
    }

    public void setMaxRequestBodyBytes(int maxRequestBodyBytes) {
        this.maxRequestBodyBytes = positive(maxRequestBodyBytes, 8 * 1024);
    }

    public int getMaxResponseBodyBytes() {
        return maxResponseBodyBytes;
    }

    public void setMaxResponseBodyBytes(int maxResponseBodyBytes) {
        this.maxResponseBodyBytes = positive(maxResponseBodyBytes, 16 * 1024);
    }

    public List<String> getExcludedPaths() {
        return excludedPaths;
    }

    public void setExcludedPaths(List<String> excludedPaths) {
        this.excludedPaths = excludedPaths == null ? new ArrayList<>() : new ArrayList<>(excludedPaths);
    }

    public Set<String> getSensitiveFields() {
        return sensitiveFields;
    }

    public void setSensitiveFields(Set<String> sensitiveFields) {
        this.sensitiveFields = sensitiveFields == null ? new LinkedHashSet<>() : new LinkedHashSet<>(sensitiveFields);
    }

    private int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}
