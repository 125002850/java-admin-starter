package com.oigit.admin.workcalendar.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "保存工作日历完整草稿请求")
public record SaveWorkCalendarDraftReqDTO(
        @NotNull @Min(2000) @Max(2099) Integer year,
        @Schema(description = "已有草稿版本 ID；首次保存为空") Long draftVersionId,
        @Min(0) @Schema(description = "已有草稿乐观锁版本；首次保存为空") Integer expectedLockVersion,
        @NotEmpty @Size(max = 8) List<@Valid WorkCalendarPeriodReqDTO> standardPeriods,
        @NotNull @Size(max = 366) List<@Valid WorkCalendarDateOverrideReqDTO> dateOverrides
) {

    public SaveWorkCalendarDraftReqDTO {
        standardPeriods = standardPeriods == null ? null : List.copyOf(standardPeriods);
        dateOverrides = dateOverrides == null ? null : List.copyOf(dateOverrides);
    }
}
