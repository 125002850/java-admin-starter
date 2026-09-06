package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "更新菜单请求")
public class MenuUpdateReqDTO extends MenuCreateReqDTO {
    @NotNull
    @Schema(description = "菜单ID", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
    public Long menuId;
}
