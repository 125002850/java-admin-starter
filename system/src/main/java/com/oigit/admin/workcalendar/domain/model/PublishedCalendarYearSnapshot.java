package com.oigit.admin.workcalendar.domain.model;

import com.oigit.admin.workcalendar.enums.EffectiveDayKind;
import java.time.LocalDate;
import java.util.Map;

public record PublishedCalendarYearSnapshot(
        int year,
        Long calendarVersionId,
        Integer calendarVersionNo,
        Map<LocalDate, EffectiveDayKind> effectiveDayKindsByDate
) {
    public PublishedCalendarYearSnapshot {
        effectiveDayKindsByDate = Map.copyOf(effectiveDayKindsByDate);
    }
}
