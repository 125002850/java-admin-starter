package com.oigit.admin.workcalendar.domain.model;



import java.util.List;

public record CalendarVersionSnapshot(
        CalendarVersion version,
        List<WorkingPeriod> standardPeriods,
        List<CalendarDateOverride> dateOverrides
) {

    public CalendarVersionSnapshot {
        standardPeriods = List.copyOf(standardPeriods);
        dateOverrides = List.copyOf(dateOverrides);
    }
}
