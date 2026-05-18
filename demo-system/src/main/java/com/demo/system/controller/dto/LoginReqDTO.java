package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "登录请求")
public class LoginReqDTO {

    @NotNull
    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @NotBlank
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank
    @Schema(description = "密码", example = "123456")
    private String password;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
