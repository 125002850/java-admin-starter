package com.oigit.admin.workcalendar.domain.model;

import java.util.List;

public record PublicationDiff(
        boolean initialPublication,
        boolean standardPeriodsChanged,
        List<WorkingPeriod> previousStandardPeriods,
        List<WorkingPeriod> nextStandardPeriods,
        int addedCount,
        int removedCount,
        int changedCount,
        List<DateOverrideChange> dateChanges
) {

    public PublicationDiff {
        previousStandardPeriods = List.copyOf(previousStandardPeriods);
        nextStandardPeriods = List.copyOf(nextStandardPeriods);
        dateChanges = List.copyOf(dateChanges);
    }
}
