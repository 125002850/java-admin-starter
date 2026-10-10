package com.oigit.admin.workcalendar.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
@Schema(description = "工作日历版本查询")
public record WorkCalendarVersionDetailReqDTO(@NotNull @Min(2000) @Max(2099) @Schema(description = "自然年", example = "2026") Integer year,
        @NotNull @Min(1) @Schema(description = "版本 ID", example = "1") Long versionId) {}
