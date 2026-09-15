package com.oigit.admin.workcalendar.domain.model;

import com.oigit.admin.workcalendar.enums.ClassificationBasis;

import java.util.List;

public record CalendarYearDetails(
        int year,
        ClassificationBasis viewBasis,
        boolean authoritative,
        CalendarVersionSnapshot activeVersion,
        CalendarVersionSnapshot draftVersion,
        List<WorkingPeriod> standardPeriods,
        List<CalendarDateOverride> dateOverrides,
        List<ResolvedCalendarDay> resolvedDays
) {

    public CalendarYearDetails {
        standardPeriods = List.copyOf(standardPeriods);
        dateOverrides = List.copyOf(dateOverrides);
        resolvedDays = List.copyOf(resolvedDays);
    }
}
