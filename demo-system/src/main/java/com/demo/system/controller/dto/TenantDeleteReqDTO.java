package com.demo.system.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "删除租户请求")
public class TenantDeleteReqDTO {

    @NotNull
    @Schema(description = "租户ID", example = "1")
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
