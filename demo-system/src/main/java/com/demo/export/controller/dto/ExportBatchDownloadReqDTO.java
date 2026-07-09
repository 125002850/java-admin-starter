package com.demo.export.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "批量下载导出记录请求")
public class ExportBatchDownloadReqDTO {

    @NotEmpty
    @Size(max = 50)
    @Schema(description = "导出记录ID列表，最多 50 条", example = "[1,2,3]")
    private List<@NotNull Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
