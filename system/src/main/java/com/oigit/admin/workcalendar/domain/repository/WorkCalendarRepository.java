package com.oigit.admin.workcalendar.domain.repository;
import com.oigit.admin.workcalendar.enums.ClassificationBasis;
import com.oigit.admin.workcalendar.enums.HolidayStatus;
import com.oigit.admin.workcalendar.enums.WorkCalendarErrorCode;
import com.oigit.admin.workcalendar.enums.WorkCalendarYearView;
import com.oigit.admin.workcalendar.enums.WorkCalendarVersionStatus;
import com.oigit.admin.workcalendar.domain.model.CalendarDateOverride;
import com.oigit.admin.workcalendar.domain.model.CalendarVersionSnapshot;
import com.oigit.admin.workcalendar.domain.model.CalendarYearDetails;
import com.oigit.admin.workcalendar.domain.model.PublicationDiff;
import com.oigit.admin.workcalendar.domain.model.PublicationPreparation;
import com.oigit.admin.workcalendar.domain.model.PublishedCalendarYearDetails;
import com.oigit.admin.workcalendar.domain.model.PublishedCalendarYearSnapshot;
import com.oigit.admin.workcalendar.domain.model.TimeClassificationResult;
import com.oigit.admin.workcalendar.domain.model.WorkingPeriod;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
public interface WorkCalendarRepository {
    CalendarYearDetails getYearDetails(int year);
    CalendarYearDetails getYearDetails(int year, WorkCalendarYearView view);
    CalendarYearDetails saveDraft(
            int year,
            Long draftVersionId,
            Integer expectedLockVersion,
            List<WorkingPeriod> standardPeriods,
            List<CalendarDateOverride> dateOverrides
    );
    CalendarYearDetails replaceDraftDateOverrides(
            int year,
            Long draftVersionId,
            Integer expectedLockVersion,
            List<CalendarDateOverride> dateOverrides
    );
    TimeClassificationResult evaluateDraft(
            Long draftVersionId,
            Integer expectedLockVersion,
            LocalDateTime localDateTime
    );
    TimeClassificationResult classify(LocalDateTime localDateTime);
    TimeClassificationResult classifyPublished(LocalDateTime localDateTime);
    Optional<PublishedCalendarYearDetails> findPublishedYearDetails(int year);
    Optional<PublishedCalendarYearSnapshot> findPublishedYearSnapshot(int year);
    PublicationPreparation preparePublication(
            Long draftVersionId,
            Integer expectedLockVersion
    );
    CalendarYearDetails publish(
            Long draftVersionId,
            Integer expectedLockVersion,
            String expectedContentHash
    );
}
