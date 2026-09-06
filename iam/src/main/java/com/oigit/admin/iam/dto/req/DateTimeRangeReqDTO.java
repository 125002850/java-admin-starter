package com.oigit.admin.iam.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "日期时间范围请求")
public class DateTimeRangeReqDTO {
    @Schema(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss", example = "2026-07-08 00:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss", example = "2026-07-08 23:59:59")
    private LocalDateTime endTime;

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
