package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "员工ID请求")
public class StaffIdReqDTO {
    @NotNull
    @Schema(description = "员工ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long staffId;

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }
}
