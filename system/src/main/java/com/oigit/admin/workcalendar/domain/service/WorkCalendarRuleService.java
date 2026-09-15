package com.oigit.admin.workcalendar.domain.service;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.workcalendar.enums.ClassificationBasis;
import com.oigit.admin.workcalendar.enums.DayTrait;
import com.oigit.admin.workcalendar.enums.EffectiveDayKind;
import com.oigit.admin.workcalendar.enums.HolidayStatus;
import com.oigit.admin.workcalendar.enums.TimeClassification;
import com.oigit.admin.workcalendar.enums.WorkCalendarDateOverrideType;
import com.oigit.admin.workcalendar.enums.WorkCalendarErrorCode;
import com.oigit.admin.workcalendar.domain.model.CalendarDateOverride;
import com.oigit.admin.workcalendar.domain.model.ResolvedCalendarDay;
import com.oigit.admin.workcalendar.domain.model.TimeClassificationResult;
import com.oigit.admin.workcalendar.domain.model.WorkingPeriod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkCalendarRuleService {

    public static final int MIN_YEAR = 2000;
    public static final int MAX_YEAR = 2099;
    public static final int MAX_PERIODS = 8;

    public void validateYear(int year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_YEAR_INVALID);
        }
    }

    public List<WorkingPeriod> normalizeStandardPeriods(List<WorkingPeriod> periods) {
        return normalizePeriods(periods, true);
    }

    public List<CalendarDateOverride> normalizeDateOverrides(
            int year,
            List<CalendarDateOverride> overrides
    ) {
        validateYear(year);
        List<CalendarDateOverride> source = overrides == null ? List.of() : overrides;
        if (source.size() > Year.of(year).length()) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DATE_OVERRIDE_INVALID);
        }

        Set<LocalDate> dates = new HashSet<>();
        List<CalendarDateOverride> normalized = new ArrayList<>(source.size());
        for (CalendarDateOverride override : source) {
            if (override == null
                    || override.date() == null
                    || override.date().getYear() != year
                    || override.type() == null
                    || !dates.add(override.date())) {
                throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DATE_OVERRIDE_INVALID);
            }

            String name = trimToNull(override.name());
            String sourceNote = trimToNull(override.sourceNote());
            List<WorkingPeriod> customPeriods = override.customPeriods();
            if (override.type() == WorkCalendarDateOverrideType.ADJUSTED_WORKDAY) {
                customPeriods = customPeriods == null ? null : normalizePeriods(customPeriods, true);
            } else {
                if (!hasText(name) || customPeriods != null) {
                    throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DATE_OVERRIDE_INVALID);
                }
            }

            normalized.add(new CalendarDateOverride(
                    override.date(),
                    override.type(),
                    name,
                    customPeriods,
                    sourceNote
            ));
        }
        normalized.sort(Comparator.comparing(CalendarDateOverride::date));
        return List.copyOf(normalized);
    }

    public ResolvedCalendarDay resolveDay(
            LocalDate date,
            List<WorkingPeriod> standardPeriods,
            Map<LocalDate, CalendarDateOverride> overridesByDate
    ) {
        boolean weekend = isWeekend(date);
        CalendarDateOverride override = overridesByDate.get(date);
        if (override == null) {
            if (weekend) {
                return new ResolvedCalendarDay(
                        date,
                        EffectiveDayKind.WEEKEND,
                        List.of(DayTrait.WEEKEND),
                        null,
                        List.of()
                );
            }
            return new ResolvedCalendarDay(
                    date,
                    EffectiveDayKind.REGULAR_WORKDAY,
                    List.of(),
                    null,
                    standardPeriods
            );
        }

        List<DayTrait> traits = new ArrayList<>();
        if (weekend) {
            traits.add(DayTrait.WEEKEND);
        }
        return switch (override.type()) {
            case ADJUSTED_WORKDAY -> {
                traits.add(DayTrait.ADJUSTED_WORKDAY);
                List<WorkingPeriod> periods = override.customPeriods() == null
                        ? standardPeriods
                        : override.customPeriods();
                yield new ResolvedCalendarDay(
                        date,
                        EffectiveDayKind.ADJUSTED_WORKDAY,
                        traits,
                        override.name(),
                        periods
                );
            }
            case PUBLIC_HOLIDAY -> {
                traits.add(DayTrait.PUBLIC_HOLIDAY);
                yield new ResolvedCalendarDay(
                        date,
                        EffectiveDayKind.PUBLIC_HOLIDAY,
                        traits,
                        override.name(),
                        List.of()
                );
            }
            case OTHER_NON_WORKING_DAY -> {
                traits.add(DayTrait.OTHER_NON_WORKING_DAY);
                yield new ResolvedCalendarDay(
                        date,
                        EffectiveDayKind.OTHER_NON_WORKING_DAY,
                        traits,
                        override.name(),
                        List.of()
                );
            }
        };
    }

    public List<ResolvedCalendarDay> resolveYear(
            int year,
            List<WorkingPeriod> standardPeriods,
            List<CalendarDateOverride> overrides
    ) {
        validateYear(year);
        Map<LocalDate, CalendarDateOverride> overridesByDate = indexOverrides(overrides);
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        int length = Year.of(year).length();
        List<ResolvedCalendarDay> days = new ArrayList<>(length);
        for (int offset = 0; offset < length; offset++) {
            LocalDate date = firstDay.plusDays(offset);
            days.add(resolveDay(date, standardPeriods, overridesByDate));
        }
        return List.copyOf(days);
    }

    public TimeClassificationResult evaluate(
            LocalDateTime localDateTime,
            List<WorkingPeriod> standardPeriods,
            List<CalendarDateOverride> overrides,
            boolean authoritative,
            ClassificationBasis basis,
            HolidayStatus holidayStatus,
            Long calendarVersionId,
            Integer calendarVersionNo
    ) {
        if (localDateTime == null) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DATE_OVERRIDE_INVALID);
        }
        ResolvedCalendarDay day = resolveDay(
                localDateTime.toLocalDate(),
                standardPeriods,
                indexOverrides(overrides)
        );

        WorkingPeriod matchedPeriod = day.effectivePeriods().stream()
                .filter(period -> !localDateTime.toLocalTime().isBefore(period.start())
                        && localDateTime.toLocalTime().isBefore(period.end()))
                .findFirst()
                .orElse(null);

        TimeClassification classification = switch (day.effectiveDayKind()) {
            case WEEKEND -> TimeClassification.WEEKEND;
            case PUBLIC_HOLIDAY -> TimeClassification.PUBLIC_HOLIDAY;
            case OTHER_NON_WORKING_DAY -> TimeClassification.OTHER_NON_WORKING_DAY;
            case REGULAR_WORKDAY, ADJUSTED_WORKDAY -> matchedPeriod == null
                    ? TimeClassification.WORKDAY_BREAK
                    : TimeClassification.WORKING_TIME;
        };

        return new TimeClassificationResult(
                localDateTime,
                classification,
                classification == TimeClassification.WORKING_TIME,
                day.effectiveDayKind(),
                day.traits(),
                day.dateName(),
                day.effectivePeriods(),
                matchedPeriod,
                authoritative,
                basis,
                holidayStatus,
                localDateTime.getYear(),
                calendarVersionId,
                calendarVersionNo
        );
    }

    private List<WorkingPeriod> normalizePeriods(List<WorkingPeriod> periods, boolean required) {
        if (periods == null || periods.isEmpty()) {
            if (required) {
                throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_PERIOD_INVALID);
            }
            return List.of();
        }
        if (periods.size() > MAX_PERIODS) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_PERIOD_INVALID);
        }

        List<WorkingPeriod> normalized = new ArrayList<>(periods.size());
        for (WorkingPeriod period : periods) {
            if (period == null
                    || period.start() == null
                    || period.end() == null
                    || !period.start().isBefore(period.end())) {
                throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_PERIOD_INVALID);
            }
            normalized.add(period);
        }
        normalized.sort(Comparator.comparing(WorkingPeriod::start));
        for (int index = 1; index < normalized.size(); index++) {
            WorkingPeriod previous = normalized.get(index - 1);
            WorkingPeriod current = normalized.get(index);
            if (current.start().isBefore(previous.end())) {
                throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_PERIOD_OVERLAP);
            }
        }
        return List.copyOf(normalized);
    }

    private Map<LocalDate, CalendarDateOverride> indexOverrides(List<CalendarDateOverride> overrides) {
        Map<LocalDate, CalendarDateOverride> indexed = new HashMap<>();
        if (overrides == null) {
            return indexed;
        }
        for (CalendarDateOverride override : overrides) {
            indexed.put(override.date(), override);
        }
        return indexed;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
