package com.demo.core.export.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "导出区间，按当前筛选和排序后的结果集 1-based 行号闭区间")
public class ExportRangeReqDTO {

    @Min(1)
    @Schema(description = "起始行号，1-based，闭区间", example = "1")
    private Long startRow;

    @Min(1)
    @Schema(description = "结束行号，1-based，闭区间", example = "5000")
    private Long endRow;

    public Long getStartRow() {
        return startRow;
    }

    public void setStartRow(Long startRow) {
        this.startRow = startRow;
    }

    public Long getEndRow() {
        return endRow;
    }

    public void setEndRow(Long endRow) {
        this.endRow = endRow;
    }
}
