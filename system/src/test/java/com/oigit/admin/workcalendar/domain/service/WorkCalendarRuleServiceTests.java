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
import com.oigit.admin.workcalendar.domain.model.TimeClassificationResult;
import com.oigit.admin.workcalendar.domain.model.WorkingPeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkCalendarRuleServiceTests {

    private final WorkCalendarRuleService ruleService = new WorkCalendarRuleService();

    @Test
    void should_use_half_open_working_period_boundaries() {
        TimeClassificationResult atStart = evaluate("2026-08-03T09:00:00", List.of());
        TimeClassificationResult atEnd = evaluate("2026-08-03T12:00:00", List.of());

        assertThat(atStart.classification()).isEqualTo(TimeClassification.WORKING_TIME);
        assertThat(atStart.working()).isTrue();
        assertThat(atStart.matchedPeriod()).isEqualTo(period("09:00", "12:00"));
        assertThat(atEnd.classification()).isEqualTo(TimeClassification.WORKDAY_BREAK);
        assertThat(atEnd.working()).isFalse();
    }

    @Test
    void public_holiday_should_override_weekend_and_keep_weekend_trait() {
        CalendarDateOverride holiday = override(
                "2026-10-04",
                WorkCalendarDateOverrideType.PUBLIC_HOLIDAY,
                "国庆节、中秋节",
                null
        );

        TimeClassificationResult result = evaluate("2026-10-04T10:00:00", List.of(holiday));

        assertThat(result.classification()).isEqualTo(TimeClassification.PUBLIC_HOLIDAY);
        assertThat(result.effectiveDayKind()).isEqualTo(EffectiveDayKind.PUBLIC_HOLIDAY);
        assertThat(result.traits()).containsExactly(DayTrait.WEEKEND, DayTrait.PUBLIC_HOLIDAY);
        assertThat(result.effectivePeriods()).isEmpty();
    }

    @Test
    void adjusted_weekend_should_inherit_standard_periods() {
        CalendarDateOverride adjusted = override(
                "2026-10-10",
                WorkCalendarDateOverrideType.ADJUSTED_WORKDAY,
                "调休工作日",
                null
        );

        TimeClassificationResult result = evaluate("2026-10-10T10:00:00", List.of(adjusted));

        assertThat(result.classification()).isEqualTo(TimeClassification.WORKING_TIME);
        assertThat(result.effectiveDayKind()).isEqualTo(EffectiveDayKind.ADJUSTED_WORKDAY);
        assertThat(result.traits()).containsExactly(DayTrait.WEEKEND, DayTrait.ADJUSTED_WORKDAY);
    }

    @Test
    void adjusted_custom_periods_should_replace_standard_periods() {
        CalendarDateOverride adjusted = override(
                "2026-10-10",
                WorkCalendarDateOverrideType.ADJUSTED_WORKDAY,
                "调休工作日",
                List.of(period("10:00", "16:00"))
        );

        TimeClassificationResult beforeCustomStart = evaluate("2026-10-10T09:30:00", List.of(adjusted));
        TimeClassificationResult duringCustomPeriod = evaluate("2026-10-10T10:30:00", List.of(adjusted));

        assertThat(beforeCustomStart.classification()).isEqualTo(TimeClassification.WORKDAY_BREAK);
        assertThat(duringCustomPeriod.classification()).isEqualTo(TimeClassification.WORKING_TIME);
        assertThat(duringCustomPeriod.effectivePeriods()).containsExactly(period("10:00", "16:00"));
    }

    @Test
    void other_non_working_day_should_be_all_day_non_working() {
        CalendarDateOverride other = override(
                "2026-08-03",
                WorkCalendarDateOverrideType.OTHER_NON_WORKING_DAY,
                "公司统一休息日",
                null
        );

        TimeClassificationResult result = evaluate("2026-08-03T10:00:00", List.of(other));

        assertThat(result.classification()).isEqualTo(TimeClassification.OTHER_NON_WORKING_DAY);
        assertThat(result.working()).isFalse();
        assertThat(result.dateName()).isEqualTo("公司统一休息日");
    }

    @Test
    void weekly_fallback_should_keep_holiday_status_unknown() {
        TimeClassificationResult result = ruleService.evaluate(
                LocalDateTime.parse("2027-08-02T10:00:00"),
                standardPeriods(),
                List.of(),
                false,
                ClassificationBasis.WEEKLY_FALLBACK,
                HolidayStatus.UNKNOWN,
                null,
                null
        );

        assertThat(result.classification()).isEqualTo(TimeClassification.WORKING_TIME);
        assertThat(result.authoritative()).isFalse();
        assertThat(result.basis()).isEqualTo(ClassificationBasis.WEEKLY_FALLBACK);
        assertThat(result.holidayStatus()).isEqualTo(HolidayStatus.UNKNOWN);
    }

    @Test
    void should_sort_periods_and_reject_overlap() {
        assertThat(ruleService.normalizeStandardPeriods(List.of(
                period("13:30", "18:00"),
                period("09:00", "12:00")
        ))).containsExactly(
                period("09:00", "12:00"),
                period("13:30", "18:00")
        );

        assertThatThrownBy(() -> ruleService.normalizeStandardPeriods(List.of(
                period("09:00", "12:00"),
                period("11:59", "18:00")
        )))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(WorkCalendarErrorCode.WORK_CALENDAR_PERIOD_OVERLAP);
    }

    @Test
    void should_reject_non_working_override_without_name() {
        assertThatThrownBy(() -> ruleService.normalizeDateOverrides(
                2026,
                List.of(override(
                        "2026-08-03",
                        WorkCalendarDateOverrideType.PUBLIC_HOLIDAY,
                        " ",
                        null
                ))
        )).isInstanceOf(BizException.class);
    }

    @Test
    void should_resolve_every_day_of_leap_year() {
        assertThat(ruleService.resolveYear(2028, standardPeriods(), List.of())).hasSize(366);
    }

    private TimeClassificationResult evaluate(String localDateTime, List<CalendarDateOverride> overrides) {
        return ruleService.evaluate(
                LocalDateTime.parse(localDateTime),
                standardPeriods(),
                overrides,
                true,
                ClassificationBasis.PUBLISHED_SNAPSHOT,
                HolidayStatus.CONFIRMED,
                1L,
                1
        );
    }

    private List<WorkingPeriod> standardPeriods() {
        return List.of(period("09:00", "12:00"), period("13:30", "18:00"));
    }

    private WorkingPeriod period(String start, String end) {
        return new WorkingPeriod(LocalTime.parse(start), LocalTime.parse(end));
    }

    private CalendarDateOverride override(
            String date,
            WorkCalendarDateOverrideType type,
            String name,
            List<WorkingPeriod> customPeriods
    ) {
        return new CalendarDateOverride(LocalDate.parse(date), type, name, customPeriods, null);
    }
}
