package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "IAM退出登录请求")
public class LogoutReqDTO {
    @Schema(description = "refresh token，可为空")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
