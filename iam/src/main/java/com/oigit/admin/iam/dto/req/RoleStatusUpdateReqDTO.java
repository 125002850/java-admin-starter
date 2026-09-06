package com.oigit.admin.iam.dto.req;

import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "角色状态更新请求")
public class RoleStatusUpdateReqDTO extends RoleIdReqDTO {
    @NotNull
    @Schema(description = "状态", example = "DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
    public IamStatus status;
}
