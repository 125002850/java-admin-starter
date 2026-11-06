package com.oigit.admin.workcalendar.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "指定工作日历版本试算请求，支持草稿、生效及历史发布版本")
public record EvaluateWorkCalendarVersionReqDTO(
        @NotNull @Min(1)
        @Schema(description = "当前查看的工作日历版本 ID", example = "1")
        Long versionId,
        @NotNull @Min(0)
        @Schema(description = "当前查看版本的乐观锁版本号", example = "0")
        Integer expectedLockVersion,
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "版本所属年度内的 Asia/Shanghai 本地时间", example = "2026-10-10 10:00:00")
        LocalDateTime localDateTime
) {
}
