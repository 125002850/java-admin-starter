package com.demo.mdm.export.controller.dto.query;

import com.demo.core.query.dto.BasePagedDynamicQueryReqDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

@Schema(description = "导出记录动态分页查询请求")
public class ExportRecordDynamicPageReqDTO extends BasePagedDynamicQueryReqDTO<
    ExportRecordDynamicCriteriaReqDTO.ConditionNode,
    ExportRecordDynamicCriteriaReqDTO.SortItem> {

    @Override
    @Valid
    @Schema(
        description = "查询条件树",
        implementation = ExportRecordDynamicCriteriaReqDTO.ConditionNode.class
    )
    public ExportRecordDynamicCriteriaReqDTO.ConditionNode getCondition() {
        return super.getCondition();
    }

    @Override
    @Valid
    @ArraySchema(
        arraySchema = @Schema(description = "排序项"),
        schema = @Schema(implementation = ExportRecordDynamicCriteriaReqDTO.SortItem.class)
    )
    public List<ExportRecordDynamicCriteriaReqDTO.SortItem> getSort() {
        return super.getSort();
    }
}
