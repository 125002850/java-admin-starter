package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "部门ID请求")
public class DeptIdReqDTO {
    @NotNull
    @Schema(description = "部门ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    public Long deptId;
}
