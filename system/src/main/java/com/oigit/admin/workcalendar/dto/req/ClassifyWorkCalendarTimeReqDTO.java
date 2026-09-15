package com.oigit.admin.workcalendar.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "工作日历生效规则时间判定请求")
public record ClassifyWorkCalendarTimeReqDTO(
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "Asia/Shanghai 本地时间", example = "2026-10-04 10:00:00")
        LocalDateTime localDateTime
) {
}
