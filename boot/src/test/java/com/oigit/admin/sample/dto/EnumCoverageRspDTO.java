package com.oigit.admin.sample.dto;

import com.oigit.admin.core.enums.EnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RenamedEnumCoverage")
public class EnumCoverageRspDTO {
    @Schema(description = "可空状态", nullable = true)
    public EnableStatusEnum status;
}
