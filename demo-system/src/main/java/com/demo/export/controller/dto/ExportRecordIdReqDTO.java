package com.demo.export.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "导出记录ID请求")
public class ExportRecordIdReqDTO {

    @NotNull
    @Schema(description = "导出记录ID", example = "1")
    private Long recordId;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }
}
