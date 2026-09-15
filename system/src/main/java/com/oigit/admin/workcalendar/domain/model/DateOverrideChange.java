package com.oigit.admin.workcalendar.domain.model;

import com.oigit.admin.workcalendar.enums.PublicationChangeType;

import java.time.LocalDate;

public record DateOverrideChange(
        LocalDate date,
        PublicationChangeType changeType,
        CalendarDateOverride before,
        CalendarDateOverride after
) {
}
