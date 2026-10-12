package com.oigit.admin.queryscene.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "单个动态查询场景的元数据")
public record QuerySceneMetadataRspDTO(
        @Schema(description = "可筛选字段，以 DSL 字段编码为键；不含仅排序字段", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<String, QuerySceneFieldRspDTO> fields,
        @Schema(description = "查询条件限制", requiredMode = Schema.RequiredMode.REQUIRED)
        QuerySceneLimitsRspDTO limits
) {
}
