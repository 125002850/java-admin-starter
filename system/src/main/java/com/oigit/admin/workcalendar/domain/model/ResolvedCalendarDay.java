package com.oigit.admin.workcalendar.domain.model;

import com.oigit.admin.workcalendar.enums.DayTrait;
import com.oigit.admin.workcalendar.enums.EffectiveDayKind;

import java.time.LocalDate;
import java.util.List;

public record ResolvedCalendarDay(
        LocalDate date,
        EffectiveDayKind effectiveDayKind,
        List<DayTrait> traits,
        String dateName,
        List<WorkingPeriod> effectivePeriods
) {

    public ResolvedCalendarDay {
        traits = List.copyOf(traits);
        effectivePeriods = List.copyOf(effectivePeriods);
    }
}
