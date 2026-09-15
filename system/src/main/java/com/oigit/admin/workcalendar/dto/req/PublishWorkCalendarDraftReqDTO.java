package com.oigit.admin.workcalendar.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "确认发布工作日历草稿请求")
public record PublishWorkCalendarDraftReqDTO(
        @NotNull @Min(1) Long draftVersionId,
        @NotNull @Min(0) Integer expectedLockVersion,
        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String contentHash
) {
}
