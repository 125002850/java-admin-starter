package com.oigit.admin.iam.dto.req;

import com.oigit.admin.iam.enums.IamStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "员工状态更新请求")
public class StaffStatusUpdateReqDTO extends StaffIdReqDTO {
    @NotNull
    @Schema(description = "状态", example = "DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
    private IamStatus status;

    public IamStatus getStatus() {
        return status;
    }

    public void setStatus(IamStatus status) {
        this.status = status;
    }
}
