package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public class LoginRspDTO {

    @Schema(description = "用户ID", example = "1001")
    private final Long userId;
    @Schema(description = "租户ID", example = "1")
    private final Long tenantId;
    @Schema(description = "用户名", example = "admin")
    private final String username;

    public LoginRspDTO(Long userId, Long tenantId, String username) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getUsername() {
        return username;
    }
}
