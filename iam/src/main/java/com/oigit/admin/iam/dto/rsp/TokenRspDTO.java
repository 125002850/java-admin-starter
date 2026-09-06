package com.oigit.admin.iam.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "IAM token响应")
public class TokenRspDTO {
    @Schema(description = "access token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;

    @Schema(description = "refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    @Schema(
            description = "access token过期时间",
            example = "2026-07-08 10:30:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime accessTokenExpiresAt;

    @Schema(
            description = "token类型",
            example = "Bearer",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String tokenType = "Bearer";

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(LocalDateTime accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
