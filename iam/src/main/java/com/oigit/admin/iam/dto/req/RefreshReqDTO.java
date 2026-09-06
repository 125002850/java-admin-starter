package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "IAM刷新token请求")
public class RefreshReqDTO {
    @NotBlank
    @Schema(description = "refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
