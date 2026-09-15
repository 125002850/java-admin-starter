package com.oigit.admin.workcalendar.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "工作日历发布差异摘要")
public record PublicationDiffRspDTO(
        Boolean initialPublication,
        Boolean standardPeriodsChanged,
        List<WorkCalendarPeriodRspDTO> previousStandardPeriods,
        List<WorkCalendarPeriodRspDTO> nextStandardPeriods,
        Integer addedCount,
        Integer removedCount,
        Integer changedCount,
        List<DateOverrideChangeRspDTO> dateChanges
) {

    public PublicationDiffRspDTO {
        previousStandardPeriods = List.copyOf(previousStandardPeriods);
        nextStandardPeriods = List.copyOf(nextStandardPeriods);
        dateChanges = List.copyOf(dateChanges);
    }
}
