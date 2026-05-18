package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "修改租户请求")
public class TenantUpdateReqDTO {

    @NotNull
    @Schema(description = "租户ID", example = "1")
    private Long id;

    @NotBlank
    @Size(min = 1, max = 128)
    @Schema(description = "新租户名称", example = "new-tenant-name")
    private String tenantName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
}
