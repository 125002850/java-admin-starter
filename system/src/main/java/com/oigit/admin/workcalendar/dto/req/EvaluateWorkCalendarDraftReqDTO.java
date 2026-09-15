package com.oigit.admin.workcalendar.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "已保存工作日历草稿试算请求")
public record EvaluateWorkCalendarDraftReqDTO(
        @NotNull @Min(1) Long draftVersionId,
        @NotNull @Min(0) Integer expectedLockVersion,
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Asia/Shanghai 本地时间", example = "2026-10-10 10:00:00")
        LocalDateTime localDateTime
) {
}
