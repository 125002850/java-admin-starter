package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "日志ID请求")
public class LogIdReqDTO {
    @NotNull
    @Schema(description = "日志ID", requiredMode = Schema.RequiredMode.REQUIRED)
    public Long logId;
}
