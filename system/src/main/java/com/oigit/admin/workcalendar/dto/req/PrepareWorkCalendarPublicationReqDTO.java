package com.oigit.admin.workcalendar.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "工作日历发布预检请求")
public record PrepareWorkCalendarPublicationReqDTO(
        @NotNull @Min(1) Long draftVersionId,
        @NotNull @Min(0) Integer expectedLockVersion
) {
}
