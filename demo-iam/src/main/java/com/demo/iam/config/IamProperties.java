package com.demo.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.iam")
public class IamProperties {

    private String jwtSecret = "change-me-change-me-change-me-change-me";
    private long accessTokenTtlMinutes = 30;
    private long refreshTokenTtlDays = 14;
    private long failureDelayMillis = 500;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
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
