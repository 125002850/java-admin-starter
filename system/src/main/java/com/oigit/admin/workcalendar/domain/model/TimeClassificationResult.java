package com.oigit.admin.workcalendar.domain.model;

import com.oigit.admin.workcalendar.enums.ClassificationBasis;
import com.oigit.admin.workcalendar.enums.DayTrait;
import com.oigit.admin.workcalendar.enums.EffectiveDayKind;
import com.oigit.admin.workcalendar.enums.HolidayStatus;
import com.oigit.admin.workcalendar.enums.TimeClassification;

import java.time.LocalDateTime;
import java.util.List;

public record TimeClassificationResult(
        LocalDateTime inputLocalDateTime,
        TimeClassification classification,
        boolean working,
        EffectiveDayKind effectiveDayKind,
        List<DayTrait> traits,
        String dateName,
        List<WorkingPeriod> effectivePeriods,
        WorkingPeriod matchedPeriod,
        boolean authoritative,
        ClassificationBasis basis,
        HolidayStatus holidayStatus,
        int calendarYear,
        Long calendarVersionId,
        Integer calendarVersionNo
) {

    public TimeClassificationResult {
        traits = List.copyOf(traits);
        effectivePeriods = List.copyOf(effectivePeriods);
    }
}
