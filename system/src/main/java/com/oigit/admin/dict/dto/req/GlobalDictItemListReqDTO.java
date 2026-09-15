package com.oigit.admin.dict.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "按类型查询全部全局字典项请求（不分页）")
public record GlobalDictItemListReqDTO(
        @NotBlank(message = "字典类型编码不能为空")
        @Schema(description = "字典类型编码", example = "EVENT_MANAGEMENT_FIELD", requiredMode = Schema.RequiredMode.REQUIRED)
        String dictTypeCode
) {
}
