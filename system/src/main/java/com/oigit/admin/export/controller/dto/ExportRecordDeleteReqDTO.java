package com.oigit.admin.export.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "导出记录删除请求（支持多选）")
public class ExportRecordDeleteReqDTO {

    @NotEmpty
    @Schema(description = "导出记录ID列表", example = "[1,2,3]")
    private List<@NotNull Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
