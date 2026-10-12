package com.oigit.admin.queryscene.dto.rsp;

import com.oigit.admin.core.query.ast.QueryOperator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "动态查询字段元数据，不包含数据库列或 SQL 实现")
public record QuerySceneFieldRspDTO(
        @Schema(description = "字段显示名称", example = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String label,
        @Schema(description = "条件节点类型", example = "text", allowableValues = {"text", "dateTime", "enum", "number", "boolean"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String conditionType,
        @Schema(description = "字段允许的比较符", requiredMode = Schema.RequiredMode.REQUIRED)
        List<QueryOperator> operators,
        @Schema(description = "码值与显示名称；非枚举或动态字典字段可为空", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<String, String> valueLabels
) {
}
