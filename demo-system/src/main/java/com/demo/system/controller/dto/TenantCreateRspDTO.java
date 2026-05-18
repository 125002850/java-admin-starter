package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建租户响应")
public class TenantCreateRspDTO {

    @Schema(description = "新建租户ID")
    private Long tenantId;

    public TenantCreateRspDTO() {}

    public TenantCreateRspDTO(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
