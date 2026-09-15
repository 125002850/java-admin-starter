package com.oigit.admin.workcalendar.dto.rsp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.oigit.admin.workcalendar.enums.WorkCalendarVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "工作日历版本摘要")
public record WorkCalendarVersionSummaryRspDTO(
        Long versionId,
        Integer versionNo,
        @Schema()
        WorkCalendarVersionStatus status,
        Integer lockVersion,
        String contentHash,
        Long publishedBy,
        String publishedByName,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime publishedAt
) {
}
