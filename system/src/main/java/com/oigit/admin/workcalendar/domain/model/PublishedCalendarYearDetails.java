package com.oigit.admin.workcalendar.domain.model;

import java.util.List;

public record PublishedCalendarYearDetails(
        int year,
        Long calendarVersionId,
        Integer calendarVersionNo,
        List<ResolvedCalendarDay> resolvedDays
) {
    public PublishedCalendarYearDetails {
        resolvedDays = List.copyOf(resolvedDays);
    }
}
