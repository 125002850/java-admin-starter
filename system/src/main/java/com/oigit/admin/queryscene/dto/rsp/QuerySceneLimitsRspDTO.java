package com.oigit.admin.queryscene.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "动态查询条件限制")
public record QuerySceneLimitsRspDTO(
        @Schema(description = "合并全部条件后的最大深度", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        int maxDepth,
        @Schema(description = "合并全部条件后的最大节点数", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        int maxNodes,
        @Schema(description = "单个 IN 或 NOT_IN 的最大值数量", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
        int maxInSize,
        @Schema(description = "场景最大复杂度评分", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        int maxComplexityScore
) {
}
