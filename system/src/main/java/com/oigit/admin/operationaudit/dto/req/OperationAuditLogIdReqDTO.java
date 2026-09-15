package com.oigit.admin.operationaudit.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "操作日志详情请求")
public class OperationAuditLogIdReqDTO {

    @NotNull
    @Positive
    @Schema(description = "操作日志 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long logId;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }
}
