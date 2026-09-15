package com.oigit.admin.operationaudit.dto.req.query;

import com.oigit.admin.core.query.dto.BasePagedDynamicQueryReqDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

@Schema(description = "操作日志动态分页请求")
public class OperationAuditLogDynamicPageReqDTO extends BasePagedDynamicQueryReqDTO<
        OperationAuditLogDynamicCriteriaReqDTO.ConditionNode,
        OperationAuditLogDynamicCriteriaReqDTO.SortItem> {

    @Override
    @Valid
    @Schema(description = "查询条件树", implementation = OperationAuditLogDynamicCriteriaReqDTO.ConditionNode.class)
    public OperationAuditLogDynamicCriteriaReqDTO.ConditionNode getCondition() {
        return super.getCondition();
    }

    @Override
    @Valid
    @ArraySchema(
            arraySchema = @Schema(description = "排序项"),
            schema = @Schema(implementation = OperationAuditLogDynamicCriteriaReqDTO.SortItem.class)
    )
    public List<OperationAuditLogDynamicCriteriaReqDTO.SortItem> getSort() {
        return super.getSort();
    }
}
