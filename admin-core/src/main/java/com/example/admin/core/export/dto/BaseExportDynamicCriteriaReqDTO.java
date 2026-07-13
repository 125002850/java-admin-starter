package com.example.admin.core.export.dto;

import com.example.admin.core.query.dto.AbstractConditionNodeDTO;
import com.example.admin.core.query.dto.BaseDynamicCriteriaReqDTO;
import com.example.admin.core.query.dto.SortItemDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "动态条件导出请求基类")
public class BaseExportDynamicCriteriaReqDTO<N extends AbstractConditionNodeDTO, S extends SortItemDTO<?>>
        extends BaseDynamicCriteriaReqDTO<N, S>
        implements ExportOptionsReqDTO {

    @Valid
    @Schema(description = "导出区间；不传时导出当前筛选和排序后的全集，传入时只导出该闭区间")
    private ExportRangeReqDTO range;

    @Schema(description = "是否打包导出；true 时后端按 chunkSize 拆分多个 CSV 并压缩为 zip", example = "false")
    private Boolean packageMode;

    @Min(1)
    @Max(5000)
    @Schema(description = "打包导出每个 CSV 的最大行数；默认 5000，最大 5000", example = "5000")
    private Integer chunkSize;

    @Override
    public ExportRangeReqDTO getRange() {
        return range;
    }

    @Override
    public void setRange(ExportRangeReqDTO range) {
        this.range = range;
    }

    @Override
    public Boolean getPackageMode() {
        return packageMode;
    }

    @Override
    public void setPackageMode(Boolean packageMode) {
        this.packageMode = packageMode;
    }

    @Override
    public Integer getChunkSize() {
        return chunkSize;
    }

    @Override
    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }
}
