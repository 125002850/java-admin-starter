package com.oigit.admin.workcalendar.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.operator.OperatorUsernameResolver;
import com.oigit.admin.workcalendar.dto.req.ClassifyWorkCalendarTimeReqDTO;
import com.oigit.admin.workcalendar.dto.rsp.DateOverrideChangeRspDTO;
import com.oigit.admin.workcalendar.dto.req.EvaluateWorkCalendarDraftReqDTO;
import com.oigit.admin.workcalendar.dto.req.PrepareWorkCalendarPublicationReqDTO;
import com.oigit.admin.workcalendar.dto.rsp.PrepareWorkCalendarPublicationRspDTO;
import com.oigit.admin.workcalendar.dto.rsp.PublicationDiffRspDTO;
import com.oigit.admin.workcalendar.dto.req.PublishWorkCalendarDraftReqDTO;
import com.oigit.admin.workcalendar.dto.rsp.ResolvedCalendarDayRspDTO;
import com.oigit.admin.workcalendar.dto.req.SaveWorkCalendarDraftReqDTO;
import com.oigit.admin.workcalendar.dto.rsp.TimeClassificationRspDTO;
import com.oigit.admin.workcalendar.dto.req.WorkCalendarDateOverrideReqDTO;
import com.oigit.admin.workcalendar.dto.rsp.WorkCalendarDateOverrideRspDTO;
import com.oigit.admin.workcalendar.dto.req.WorkCalendarImportPayload;
import com.oigit.admin.workcalendar.dto.req.WorkCalendarPeriodReqDTO;
import com.oigit.admin.workcalendar.dto.rsp.WorkCalendarPeriodRspDTO;
import com.oigit.admin.workcalendar.dto.rsp.WorkCalendarVersionSummaryRspDTO;
import com.oigit.admin.workcalendar.dto.rsp.WorkCalendarYearDetailRspDTO;
import com.oigit.admin.workcalendar.enums.WorkCalendarErrorCode;
import com.oigit.admin.workcalendar.enums.WorkCalendarYearView;
import com.oigit.admin.workcalendar.domain.gateway.WorkCalendarCodec;
import com.oigit.admin.workcalendar.domain.repository.WorkCalendarRepository;
import com.oigit.admin.workcalendar.domain.model.CalendarDateOverride;
import com.oigit.admin.workcalendar.domain.model.CalendarVersionSnapshot;
import com.oigit.admin.workcalendar.domain.model.CalendarYearDetails;
import com.oigit.admin.workcalendar.domain.model.DateOverrideChange;
import com.oigit.admin.workcalendar.domain.model.PublicationDiff;
import com.oigit.admin.workcalendar.domain.model.PublicationPreparation;
import com.oigit.admin.workcalendar.domain.model.ResolvedCalendarDay;
import com.oigit.admin.workcalendar.domain.model.TimeClassificationResult;
import com.oigit.admin.workcalendar.domain.model.WorkingPeriod;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkCalendarAppService {

    private static final long MAX_IMPORT_BYTES = 1024L * 1024L;

    private final WorkCalendarRepository workCalendarService;
    private final WorkCalendarCodec jsonCodec;
    private final ObjectMapper strictImportMapper;
    private final Validator validator;
    private final OperatorUsernameResolver operatorUsernameResolver;

    public WorkCalendarAppService(
            WorkCalendarRepository workCalendarService,
            WorkCalendarCodec jsonCodec,
            ObjectMapper objectMapper,
            Validator validator,
            OperatorUsernameResolver operatorUsernameResolver
    ) {
        this.workCalendarService = workCalendarService;
        this.jsonCodec = jsonCodec;
        this.strictImportMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        this.validator = validator;
        this.operatorUsernameResolver = operatorUsernameResolver;
    }

    @Transactional(readOnly = true)
    public WorkCalendarYearDetailRspDTO fetchYearDetail(int year, WorkCalendarYearView view) {
        return toYearDetailRsp(workCalendarService.getYearDetails(year, view));
    }

    @Transactional
    public WorkCalendarYearDetailRspDTO saveDraft(SaveWorkCalendarDraftReqDTO reqDTO) {
        CalendarYearDetails details = workCalendarService.saveDraft(
                reqDTO.year(),
                reqDTO.draftVersionId(),
                reqDTO.expectedLockVersion(),
                reqDTO.standardPeriods().stream().map(this::toDomain).toList(),
                reqDTO.dateOverrides().stream().map(this::toDomain).toList()
        );
        return toYearDetailRsp(details);
    }

    @Transactional
    public WorkCalendarYearDetailRspDTO importDraft(
            int year,
            Long draftVersionId,
            Integer expectedLockVersion,
            byte[] file
    ) {
        WorkCalendarImportPayload payload = readImportPayload(file);
        if (payload.year() != year) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_IMPORT_INVALID);
        }
        List<CalendarDateOverride> overrides = payload.dateOverrides().stream()
                .map(item -> toDomain(item, payload.source()))
                .toList();
        return toYearDetailRsp(workCalendarService.replaceDraftDateOverrides(
                year,
                draftVersionId,
                expectedLockVersion,
                overrides
        ));
    }

    @Transactional(readOnly = true)
    public TimeClassificationRspDTO evaluateDraft(EvaluateWorkCalendarDraftReqDTO reqDTO) {
        return toTimeClassificationRsp(workCalendarService.evaluateDraft(
                reqDTO.draftVersionId(),
                reqDTO.expectedLockVersion(),
                reqDTO.localDateTime()
        ));
    }

    @Transactional(readOnly = true)
    public PrepareWorkCalendarPublicationRspDTO preparePublication(
            PrepareWorkCalendarPublicationReqDTO reqDTO
    ) {
        PublicationPreparation preparation = workCalendarService.preparePublication(
                reqDTO.draftVersionId(),
                reqDTO.expectedLockVersion()
        );
        return new PrepareWorkCalendarPublicationRspDTO(
                preparation.draftVersionId(),
                preparation.draftLockVersion(),
                preparation.contentHash(),
                true,
                List.of(),
                toDiffRsp(preparation.diff())
        );
    }

    @Transactional
    public WorkCalendarYearDetailRspDTO publish(PublishWorkCalendarDraftReqDTO reqDTO) {
        return toYearDetailRsp(workCalendarService.publish(
                reqDTO.draftVersionId(),
                reqDTO.expectedLockVersion(),
                reqDTO.contentHash()
        ));
    }

    @Transactional(readOnly = true)
    public TimeClassificationRspDTO classify(ClassifyWorkCalendarTimeReqDTO reqDTO) {
        return toTimeClassificationRsp(workCalendarService.classify(reqDTO.localDateTime()));
    }

    private WorkCalendarImportPayload readImportPayload(byte[] file) {
        if (file == null || file.length == 0 || file.length > MAX_IMPORT_BYTES) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_IMPORT_INVALID);
        }
        try {
            WorkCalendarImportPayload payload = strictImportMapper.readValue(
                    file,
                    WorkCalendarImportPayload.class
            );
            Set<ConstraintViolation<WorkCalendarImportPayload>> violations = validator.validate(payload);
            if (!violations.isEmpty()) {
                throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_IMPORT_INVALID);
            }
            return payload;
        } catch (IOException exception) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_IMPORT_INVALID);
        }
    }

    private WorkCalendarYearDetailRspDTO toYearDetailRsp(CalendarYearDetails details) {
        Set<Long> publisherIds = new HashSet<>();
        collectPublisherId(details.activeVersion(), publisherIds);
        collectPublisherId(details.draftVersion(), publisherIds);
        Map<Long, String> usernames = operatorUsernameResolver.resolveUsernames(publisherIds);
        return new WorkCalendarYearDetailRspDTO(
                details.year(),
                "Asia/Shanghai",
                details.viewBasis(),
                details.authoritative(),
                toVersionSummary(details.activeVersion(), usernames),
                toVersionSummary(details.draftVersion(), usernames),
                details.standardPeriods().stream().map(this::toRsp).toList(),
                details.dateOverrides().stream().map(this::toRsp).toList(),
                details.resolvedDays().stream().map(this::toRsp).toList()
        );
    }

    private WorkCalendarVersionSummaryRspDTO toVersionSummary(
            CalendarVersionSnapshot snapshot,
            Map<Long, String> usernames
    ) {
        if (snapshot == null) {
            return null;
        }
        var version = snapshot.version();
        return new WorkCalendarVersionSummaryRspDTO(
                version.getId(),
                version.getVersionNo(),
                version.getStatus(),
                version.getVersion(),
                version.getContentHash(),
                version.getPublishedBy(),
                version.getPublishedBy() == null ? null : usernames.get(version.getPublishedBy()),
                version.getPublishedAt()
        );
    }

    private ResolvedCalendarDayRspDTO toRsp(ResolvedCalendarDay day) {
        return new ResolvedCalendarDayRspDTO(
                day.date(),
                day.date().getDayOfWeek().getValue(),
                day.effectiveDayKind(),
                day.traits(),
                day.dateName(),
                day.effectivePeriods().stream().map(this::toRsp).toList()
        );
    }

    private TimeClassificationRspDTO toTimeClassificationRsp(TimeClassificationResult result) {
        return new TimeClassificationRspDTO(
                result.inputLocalDateTime(),
                "Asia/Shanghai",
                result.classification(),
                result.working(),
                result.effectiveDayKind(),
                result.traits(),
                result.dateName(),
                result.effectivePeriods().stream().map(this::toRsp).toList(),
                result.matchedPeriod() == null ? null : toRsp(result.matchedPeriod()),
                result.authoritative(),
                result.basis(),
                result.holidayStatus(),
                result.calendarYear(),
                result.calendarVersionId(),
                result.calendarVersionNo()
        );
    }

    private PublicationDiffRspDTO toDiffRsp(PublicationDiff diff) {
        return new PublicationDiffRspDTO(
                diff.initialPublication(),
                diff.standardPeriodsChanged(),
                diff.previousStandardPeriods().stream().map(this::toRsp).toList(),
                diff.nextStandardPeriods().stream().map(this::toRsp).toList(),
                diff.addedCount(),
                diff.removedCount(),
                diff.changedCount(),
                diff.dateChanges().stream().map(this::toRsp).toList()
        );
    }

    private DateOverrideChangeRspDTO toRsp(DateOverrideChange change) {
        return new DateOverrideChangeRspDTO(
                change.date(),
                change.changeType(),
                change.before() == null ? null : toRsp(change.before()),
                change.after() == null ? null : toRsp(change.after())
        );
    }

    private CalendarDateOverride toDomain(WorkCalendarDateOverrideReqDTO dto) {
        return toDomain(dto, null);
    }

    private CalendarDateOverride toDomain(WorkCalendarDateOverrideReqDTO dto, String defaultSource) {
        String sourceNote = StringUtils.hasText(dto.sourceNote()) ? dto.sourceNote() : defaultSource;
        return new CalendarDateOverride(
                dto.date(),
                dto.type(),
                dto.name(),
                dto.customPeriods() == null
                        ? null
                        : dto.customPeriods().stream().map(this::toDomain).toList(),
                sourceNote
        );
    }

    private WorkingPeriod toDomain(WorkCalendarPeriodReqDTO dto) {
        return jsonCodec.parsePeriod(dto.start(), dto.end());
    }

    private WorkCalendarPeriodRspDTO toRsp(WorkingPeriod period) {
        return new WorkCalendarPeriodRspDTO(
                jsonCodec.formatTime(period.start()),
                jsonCodec.formatTime(period.end())
        );
    }

    private WorkCalendarDateOverrideRspDTO toRsp(CalendarDateOverride override) {
        return new WorkCalendarDateOverrideRspDTO(
                override.date(),
                override.type(),
                override.name(),
                override.customPeriods() == null
                        ? null
                        : override.customPeriods().stream().map(this::toRsp).toList(),
                override.sourceNote()
        );
    }

    private void collectPublisherId(CalendarVersionSnapshot snapshot, Set<Long> publisherIds) {
        if (snapshot != null && snapshot.version().getPublishedBy() != null) {
            publisherIds.add(snapshot.version().getPublishedBy());
        }
    }
}
