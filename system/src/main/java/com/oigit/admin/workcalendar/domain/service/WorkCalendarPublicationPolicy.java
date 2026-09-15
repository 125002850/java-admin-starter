package com.oigit.admin.workcalendar.domain.service;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.workcalendar.enums.PublicationChangeType;
import com.oigit.admin.workcalendar.enums.WorkCalendarErrorCode;
import com.oigit.admin.workcalendar.domain.model.CalendarDateOverride;
import com.oigit.admin.workcalendar.domain.model.CalendarVersionSnapshot;
import com.oigit.admin.workcalendar.domain.model.DateOverrideChange;
import com.oigit.admin.workcalendar.domain.model.PublicationDiff;
import com.oigit.admin.workcalendar.domain.model.WorkingPeriod;
import java.time.LocalDate;
import com.oigit.admin.workcalendar.domain.gateway.WorkCalendarCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WorkCalendarPublicationPolicy {

    private final WorkCalendarRuleService ruleService;
    private final WorkCalendarCodec jsonCodec;

    public WorkCalendarPublicationPolicy(WorkCalendarRuleService ruleService, WorkCalendarCodec jsonCodec) {
        this.ruleService = ruleService;
        this.jsonCodec = jsonCodec;
    }

    public void validatePublishable(CalendarVersionSnapshot snapshot) {
        ruleService.normalizeStandardPeriods(snapshot.standardPeriods());
        ruleService.normalizeDateOverrides(snapshot.version().getCalendarYear(), snapshot.dateOverrides());
        if (!Objects.equals(
                snapshot.version().getContentHash(),
                jsonCodec.snapshotHash(
                        snapshot.version().getCalendarYear(),
                        snapshot.standardPeriods(),
                        snapshot.dateOverrides()
                )
        )) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_PUBLISH_VALIDATION_FAILED);
        }
        ruleService.resolveYear(
                snapshot.version().getCalendarYear(),
                snapshot.standardPeriods(),
                snapshot.dateOverrides()
        );
    }

    public PublicationDiff diff(CalendarVersionSnapshot active, CalendarVersionSnapshot draft) {
        List<WorkingPeriod> previousPeriods = active == null ? List.of() : active.standardPeriods();
        Map<LocalDate, CalendarDateOverride> previousByDate = indexByDate(
                active == null ? List.of() : active.dateOverrides()
        );
        Map<LocalDate, CalendarDateOverride> nextByDate = indexByDate(draft.dateOverrides());
        List<LocalDate> dates = new ArrayList<>();
        dates.addAll(previousByDate.keySet());
        nextByDate.keySet().stream().filter(date -> !previousByDate.containsKey(date)).forEach(dates::add);
        dates.sort(LocalDate::compareTo);

        int added = 0;
        int removed = 0;
        int changed = 0;
        List<DateOverrideChange> changes = new ArrayList<>();
        for (LocalDate date : dates) {
            CalendarDateOverride before = previousByDate.get(date);
            CalendarDateOverride after = nextByDate.get(date);
            if (Objects.equals(before, after)) {
                continue;
            }
            PublicationChangeType type;
            if (before == null) {
                type = PublicationChangeType.ADDED;
                added++;
            } else if (after == null) {
                type = PublicationChangeType.REMOVED;
                removed++;
            } else {
                type = PublicationChangeType.CHANGED;
                changed++;
            }
            changes.add(new DateOverrideChange(date, type, before, after));
        }
        return new PublicationDiff(
                active == null,
                !Objects.equals(previousPeriods, draft.standardPeriods()),
                previousPeriods,
                draft.standardPeriods(),
                added,
                removed,
                changed,
                changes
        );
    }

    private Map<LocalDate, CalendarDateOverride> indexByDate(List<CalendarDateOverride> overrides) {
        Map<LocalDate, CalendarDateOverride> indexed = new HashMap<>();
        for (CalendarDateOverride override : overrides) {
            indexed.put(override.date(), override);
        }
        return indexed;
    }
}
