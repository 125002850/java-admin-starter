package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "创建租户请求")
public class TenantCreateReqDTO {

    @NotBlank
    @Size(min = 1, max = 128)
    @Schema(description = "租户名称", example = "my-tenant")
    private String tenantName;

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
}
