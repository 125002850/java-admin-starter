package com.example.admin.iam.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.iam")
public class IamProperties {

    private static final String[] KNOWN_DEFAULT_SECRETS = {
            "change-me-change-me-change-me-change-me"
    };

    @NotBlank(message = "platform.iam.jwt-secret must be configured (set IAM_JWT_SECRET environment variable)")
    @Size(min = 32, message = "platform.iam.jwt-secret must be at least 32 characters")
    private String jwtSecret;
    private long accessTokenTtlMinutes = 30;
    private long refreshTokenTtlDays = 14;
    private long failureDelayMillis = 500;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public void assertSecretNotDefault() {
        if (jwtSecret == null) {
            throw new IllegalStateException(
                    "platform.iam.jwt-secret is not configured. Set IAM_JWT_SECRET environment variable to a strong secret (min 32 chars).");
        }
        for (String known : KNOWN_DEFAULT_SECRETS) {
            if (known.equals(jwtSecret)) {
                throw new IllegalStateException(
                        "platform.iam.jwt-secret is set to a known default value. Refusing to start. Set IAM_JWT_SECRET environment variable to a unique strong secret.");
            }
        }
    }

    public long getAccessTokenTtlMinutes() {
        return accessTokenTtlMinutes;
    }

    public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) {
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public long getRefreshTokenTtlDays() {
        return refreshTokenTtlDays;
    }

    public void setRefreshTokenTtlDays(long refreshTokenTtlDays) {
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public long getFailureDelayMillis() {
        return failureDelayMillis;
    }

    public void setFailureDelayMillis(long failureDelayMillis) {
        this.failureDelayMillis = failureDelayMillis;
    }
}
