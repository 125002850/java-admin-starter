package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "更新角色请求")
public class RoleUpdateReqDTO extends RoleCreateReqDTO {
    @NotNull
    @Schema(description = "角色ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    public Long roleId;
}
