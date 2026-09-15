package com.oigit.admin.workcalendar.dto.rsp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.oigit.admin.workcalendar.enums.WorkCalendarDateOverrideType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "工作日历特殊日期响应")
public record WorkCalendarDateOverrideRspDTO(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @Schema()
        WorkCalendarDateOverrideType type,
        String name,
        List<WorkCalendarPeriodRspDTO> customPeriods,
        String sourceNote
) {

    public WorkCalendarDateOverrideRspDTO {
        customPeriods = customPeriods == null ? null : List.copyOf(customPeriods);
    }
}
