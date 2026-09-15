package com.oigit.admin.workcalendar.dto.rsp;

import com.oigit.admin.workcalendar.enums.ClassificationBasis;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "工作日历年度详情")
public record WorkCalendarYearDetailRspDTO(
        Integer year,
        @Schema(description = "固定时区", allowableValues = "Asia/Shanghai") String zoneId,
        @Schema()
        ClassificationBasis viewBasis,
        Boolean authoritative,
        WorkCalendarVersionSummaryRspDTO activeVersion,
        WorkCalendarVersionSummaryRspDTO draftVersion,
        List<WorkCalendarPeriodRspDTO> standardPeriods,
        List<WorkCalendarDateOverrideRspDTO> dateOverrides,
        List<ResolvedCalendarDayRspDTO> resolvedDays
) {

    public WorkCalendarYearDetailRspDTO {
        standardPeriods = List.copyOf(standardPeriods);
        dateOverrides = List.copyOf(dateOverrides);
        resolvedDays = List.copyOf(resolvedDays);
    }
}
