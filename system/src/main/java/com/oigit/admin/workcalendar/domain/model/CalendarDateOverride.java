package com.oigit.admin.workcalendar.domain.model;

import com.oigit.admin.workcalendar.enums.WorkCalendarDateOverrideType;

import java.time.LocalDate;
import java.util.List;

public record CalendarDateOverride(
        LocalDate date,
        WorkCalendarDateOverrideType type,
        String name,
        List<WorkingPeriod> customPeriods,
        String sourceNote
) {

    public CalendarDateOverride {
        customPeriods = customPeriods == null ? null : List.copyOf(customPeriods);
    }
}
