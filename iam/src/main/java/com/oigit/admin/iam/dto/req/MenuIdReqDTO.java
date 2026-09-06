package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "菜单ID请求")
public class MenuIdReqDTO {
    @NotNull
    @Schema(description = "菜单ID", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
    public Long menuId;
}
