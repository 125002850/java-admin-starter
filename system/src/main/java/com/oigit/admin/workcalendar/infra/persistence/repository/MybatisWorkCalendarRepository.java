package com.oigit.admin.workcalendar.infra.persistence.repository;

import com.oigit.admin.workcalendar.infra.serialization.JacksonWorkCalendarCodec;
import com.oigit.admin.workcalendar.domain.service.WorkCalendarRuleService;
import com.oigit.admin.workcalendar.domain.service.WorkCalendarPublicationPolicy;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.workcalendar.enums.ClassificationBasis;
import com.oigit.admin.workcalendar.enums.HolidayStatus;
import com.oigit.admin.workcalendar.enums.WorkCalendarErrorCode;
import com.oigit.admin.workcalendar.enums.WorkCalendarYearView;
import com.oigit.admin.workcalendar.enums.WorkCalendarVersionStatus;
import com.oigit.admin.workcalendar.infra.persistence.entity.WorkCalendarVersionEntity;
import com.oigit.admin.workcalendar.infra.persistence.entity.WorkCalendarYearEntity;
import com.oigit.admin.workcalendar.infra.persistence.mapper.WorkCalendarDateOverrideMapper;
import com.oigit.admin.workcalendar.infra.persistence.mapper.WorkCalendarVersionMapper;
import com.oigit.admin.workcalendar.infra.persistence.mapper.WorkCalendarYearMapper;
import com.oigit.admin.workcalendar.domain.model.CalendarDateOverride;
import com.oigit.admin.workcalendar.domain.model.CalendarVersionSnapshot;
import com.oigit.admin.workcalendar.domain.model.CalendarYearDetails;
import com.oigit.admin.workcalendar.domain.model.PublicationDiff;
import com.oigit.admin.workcalendar.domain.model.PublicationPreparation;
import com.oigit.admin.workcalendar.domain.model.PublishedCalendarYearDetails;
import com.oigit.admin.workcalendar.domain.model.PublishedCalendarYearSnapshot;
import com.oigit.admin.workcalendar.domain.model.TimeClassificationResult;
import com.oigit.admin.workcalendar.domain.model.WorkingPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MybatisWorkCalendarRepository implements com.oigit.admin.workcalendar.domain.repository.WorkCalendarRepository {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisWorkCalendarRepository.class);

    private final WorkCalendarYearMapper yearMapper;
    private final WorkCalendarVersionMapper versionMapper;
    private final WorkCalendarRuleService ruleService;
    private final JacksonWorkCalendarCodec jsonCodec;
    private final WorkCalendarSnapshotStore snapshotStore;
    private final WorkCalendarPublicationPolicy publicationPolicy;

    public MybatisWorkCalendarRepository(
            WorkCalendarYearMapper yearMapper,
            WorkCalendarVersionMapper versionMapper,
            WorkCalendarDateOverrideMapper dateOverrideMapper,
            WorkCalendarRuleService ruleService,
            JacksonWorkCalendarCodec jsonCodec
    ) {
        this.yearMapper = yearMapper;
        this.versionMapper = versionMapper;
        this.ruleService = ruleService;
        this.jsonCodec = jsonCodec;
        this.snapshotStore = new WorkCalendarSnapshotStore(
                yearMapper, versionMapper, dateOverrideMapper, jsonCodec);
        this.publicationPolicy = new WorkCalendarPublicationPolicy(ruleService, jsonCodec);
    }

    public CalendarYearDetails getYearDetails(int year) {
        return getYearDetails(year, null);
    }

    public CalendarYearDetails getYearDetails(int year, WorkCalendarYearView view) {
        ruleService.validateYear(year);
        WorkCalendarYearEntity yearEntity = snapshotStore.findYear(year);
        CalendarVersionSnapshot active = yearEntity == null
                ? null
                : snapshotStore.loadSnapshotIfPresent(yearEntity.getActiveVersionId());
        CalendarVersionSnapshot draft = yearEntity == null
                ? null
                : snapshotStore.loadSnapshotIfPresent(yearEntity.getDraftVersionId());

        ClassificationBasis basis;
        boolean authoritative;
        CalendarVersionSnapshot display;
        if (view == WorkCalendarYearView.ACTIVE && active == null) {
            basis = ClassificationBasis.WEEKLY_FALLBACK;
            authoritative = false;
            display = snapshotStore.loadLatestPublishedSnapshot();
        } else if (view == WorkCalendarYearView.ACTIVE) {
            basis = ClassificationBasis.PUBLISHED_SNAPSHOT;
            authoritative = true;
            display = active;
        } else if (draft != null) {
            basis = ClassificationBasis.SAVED_DRAFT;
            authoritative = false;
            display = draft;
        } else if (active != null) {
            basis = ClassificationBasis.PUBLISHED_SNAPSHOT;
            authoritative = true;
            display = active;
        } else {
            basis = ClassificationBasis.WEEKLY_FALLBACK;
            authoritative = false;
            display = snapshotStore.loadLatestPublishedSnapshot();
        }

        List<WorkingPeriod> periods = display == null ? List.of() : display.standardPeriods();
        List<CalendarDateOverride> overrides = display == null || basis == ClassificationBasis.WEEKLY_FALLBACK
                ? List.of()
                : display.dateOverrides();
        return new CalendarYearDetails(
                year,
                basis,
                authoritative,
                active,
                draft,
                periods,
                overrides,
                ruleService.resolveYear(year, periods, overrides)
        );
    }

    public CalendarYearDetails saveDraft(
            int year,
            Long draftVersionId,
            Integer expectedLockVersion,
            List<WorkingPeriod> standardPeriods,
            List<CalendarDateOverride> dateOverrides
    ) {
        List<WorkingPeriod> normalizedPeriods = ruleService.normalizeStandardPeriods(standardPeriods);
        List<CalendarDateOverride> normalizedOverrides = ruleService.normalizeDateOverrides(year, dateOverrides);
        String contentHash = jsonCodec.snapshotHash(year, normalizedPeriods, normalizedOverrides);

        WorkCalendarYearEntity yearEntity = getOrCreateYearForUpdate(year);
        if (yearEntity.getDraftVersionId() == null) {
            if (draftVersionId != null || expectedLockVersion != null) {
                throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
            }
            WorkCalendarVersionEntity draft = new WorkCalendarVersionEntity();
            draft.setCalendarYearId(yearEntity.getId());
            draft.setCalendarYear(year);
            draft.setVersionNo(snapshotStore.nextVersionNo(yearEntity.getId()));
            draft.setStatus(WorkCalendarVersionStatus.DRAFT);
            draft.setStandardPeriodsJson(jsonCodec.encodePeriods(normalizedPeriods));
            draft.setContentHash(contentHash);
            versionMapper.insert(draft);
            snapshotStore.replaceDateOverrides(draft.getId(), normalizedOverrides);

            yearEntity.setDraftVersionId(draft.getId());
            if (yearMapper.updateById(yearEntity) != 1) {
                throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
            }
        } else {
            WorkCalendarVersionEntity draft = requireCurrentDraft(
                    yearEntity,
                    draftVersionId,
                    expectedLockVersion
            );
            draft.setStandardPeriodsJson(jsonCodec.encodePeriods(normalizedPeriods));
            draft.setContentHash(contentHash);
            if (versionMapper.updateById(draft) != 1) {
                throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
            }
            snapshotStore.replaceDateOverrides(draft.getId(), normalizedOverrides);
        }
        return getYearDetails(year);
    }

    public CalendarYearDetails replaceDraftDateOverrides(
            int year,
            Long draftVersionId,
            Integer expectedLockVersion,
            List<CalendarDateOverride> dateOverrides
    ) {
        List<CalendarDateOverride> normalizedOverrides = ruleService.normalizeDateOverrides(year, dateOverrides);
        WorkCalendarYearEntity yearEntity = requireYearForUpdate(year);
        WorkCalendarVersionEntity draft = requireCurrentDraft(yearEntity, draftVersionId, expectedLockVersion);
        List<WorkingPeriod> periods = ruleService.normalizeStandardPeriods(
                jsonCodec.decodePeriods(draft.getStandardPeriodsJson())
        );
        draft.setContentHash(jsonCodec.snapshotHash(year, periods, normalizedOverrides));
        if (versionMapper.updateById(draft) != 1) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
        }
        snapshotStore.replaceDateOverrides(draft.getId(), normalizedOverrides);
        LOGGER.info(
                "event=work_calendar_imported calendarYear={} draftVersionId={} overrideCount={} operatorId={}",
                year,
                draft.getId(),
                normalizedOverrides.size(),
                currentOperatorId()
        );
        return getYearDetails(year);
    }

    public TimeClassificationResult evaluateDraft(
            Long draftVersionId,
            Integer expectedLockVersion,
            LocalDateTime localDateTime
    ) {
        WorkCalendarVersionEntity draft = requireDraft(draftVersionId, expectedLockVersion);
        if (localDateTime == null || localDateTime.getYear() != draft.getCalendarYear()) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DATE_OVERRIDE_INVALID);
        }
        CalendarVersionSnapshot snapshot = snapshotStore.loadSnapshot(draft);
        return ruleService.evaluate(
                localDateTime,
                snapshot.standardPeriods(),
                snapshot.dateOverrides(),
                false,
                ClassificationBasis.SAVED_DRAFT,
                HolidayStatus.CANDIDATE,
                draft.getId(),
                draft.getVersionNo()
        );
    }

    public TimeClassificationResult classify(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DATE_OVERRIDE_INVALID);
        }
        int year = localDateTime.getYear();
        ruleService.validateYear(year);
        WorkCalendarYearEntity yearEntity = snapshotStore.findYear(year);
        CalendarVersionSnapshot active = yearEntity == null
                ? null
                : snapshotStore.loadSnapshotIfPresent(yearEntity.getActiveVersionId());
        if (active != null) {
            return ruleService.evaluate(
                    localDateTime,
                    active.standardPeriods(),
                    active.dateOverrides(),
                    true,
                    ClassificationBasis.PUBLISHED_SNAPSHOT,
                    HolidayStatus.CONFIRMED,
                    active.version().getId(),
                    active.version().getVersionNo()
            );
        }

        CalendarVersionSnapshot baseline = snapshotStore.loadLatestPublishedSnapshot();
        if (baseline == null) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_BASELINE_MISSING);
        }
        LOGGER.warn(
                "event=work_calendar_provisional_inference targetYear={} inputLocalDateTime={} "
                        + "sourceVersionId={} sourceCalendarYear={}",
                year,
                localDateTime,
                baseline.version().getId(),
                baseline.version().getCalendarYear()
        );
        return ruleService.evaluate(
                localDateTime,
                baseline.standardPeriods(),
                List.of(),
                false,
                ClassificationBasis.WEEKLY_FALLBACK,
                HolidayStatus.UNKNOWN,
                null,
                null
        );
    }

    /** 对外生产查询只使用目标年度的活动发布版本，不执行跨年度临时推导。 */
    public TimeClassificationResult classifyPublished(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DATE_OVERRIDE_INVALID);
        }
        int year = localDateTime.getYear();
        ruleService.validateYear(year);
        CalendarVersionSnapshot active = findPublishedSnapshot(year)
                .orElseThrow(() -> new BizException(
                        WorkCalendarErrorCode.WORK_CALENDAR_PUBLISHED_YEAR_MISSING));
        return ruleService.evaluate(
                localDateTime,
                active.standardPeriods(),
                active.dateOverrides(),
                true,
                ClassificationBasis.PUBLISHED_SNAPSHOT,
                HolidayStatus.CONFIRMED,
                active.version().getId(),
                active.version().getVersionNo()
        );
    }

    /** 对外范围查询读取不可变的年度发布快照；未发布年度由调用边界 fail-closed。 */
    public Optional<PublishedCalendarYearDetails> findPublishedYearDetails(int year) {
        ruleService.validateYear(year);
        return findPublishedSnapshot(year).map(active -> new PublishedCalendarYearDetails(
                year,
                active.version().getId(),
                active.version().getVersionNo(),
                ruleService.resolveYear(year, active.standardPeriods(), active.dateOverrides())
        ));
    }

    /** 为内部业务冻结读取某一年度当前活动发布版本；未发布年度不做临时推导。 */
    public Optional<PublishedCalendarYearSnapshot> findPublishedYearSnapshot(int year) {
        return findPublishedYearDetails(year).map(details -> {
            var dayKinds = details.resolvedDays().stream()
                .collect(Collectors.toUnmodifiableMap(
                        com.oigit.admin.workcalendar.domain.model.ResolvedCalendarDay::date,
                        com.oigit.admin.workcalendar.domain.model.ResolvedCalendarDay::effectiveDayKind));
            return new PublishedCalendarYearSnapshot(
                    details.year(),
                    details.calendarVersionId(),
                    details.calendarVersionNo(),
                    dayKinds);
        });
    }

    private Optional<CalendarVersionSnapshot> findPublishedSnapshot(int year) {
        WorkCalendarYearEntity yearEntity = snapshotStore.findYear(year);
        CalendarVersionSnapshot active = yearEntity == null
                ? null : snapshotStore.loadSnapshotIfPresent(yearEntity.getActiveVersionId());
        return active == null || active.version().getStatus() != WorkCalendarVersionStatus.PUBLISHED
                ? Optional.empty() : Optional.of(active);
    }

    public PublicationPreparation preparePublication(
            Long draftVersionId,
            Integer expectedLockVersion
    ) {
        WorkCalendarVersionEntity draft = requireDraft(draftVersionId, expectedLockVersion);
        CalendarVersionSnapshot draftSnapshot = snapshotStore.loadSnapshot(draft);
        publicationPolicy.validatePublishable(draftSnapshot);

        WorkCalendarYearEntity yearEntity = yearMapper.selectById(draft.getCalendarYearId());
        if (yearEntity == null || !Objects.equals(yearEntity.getDraftVersionId(), draft.getId())) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
        }
        CalendarVersionSnapshot active = snapshotStore.loadSnapshotIfPresent(yearEntity.getActiveVersionId());
        return new PublicationPreparation(
                draft.getId(),
                draft.getVersion(),
                draft.getContentHash(),
                publicationPolicy.diff(active, draftSnapshot)
        );
    }

    public CalendarYearDetails publish(
            Long draftVersionId,
            Integer expectedLockVersion,
            String expectedContentHash
    ) {
        WorkCalendarVersionEntity draftCandidate = requireDraft(draftVersionId, expectedLockVersion);
        WorkCalendarYearEntity yearEntity = requireYearForUpdate(draftCandidate.getCalendarYear());
        WorkCalendarVersionEntity draft = requireCurrentDraft(
                yearEntity,
                draftVersionId,
                expectedLockVersion
        );
        if (!Objects.equals(draft.getContentHash(), expectedContentHash)) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
        }

        CalendarVersionSnapshot draftSnapshot = snapshotStore.loadSnapshot(draft);
        publicationPolicy.validatePublishable(draftSnapshot);
        CalendarVersionSnapshot previousActive = snapshotStore.loadSnapshotIfPresent(yearEntity.getActiveVersionId());
        PublicationDiff publicationDiff = publicationPolicy.diff(previousActive, draftSnapshot);

        Long operatorId = currentOperatorId();
        draft.setStatus(WorkCalendarVersionStatus.PUBLISHED);
        draft.setPublishedBy(operatorId);
        draft.setPublishedAt(LocalDateTime.now(BUSINESS_ZONE));
        if (versionMapper.updateById(draft) != 1) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
        }

        Long previousActiveVersionId = yearEntity.getActiveVersionId();
        if (yearMapper.activatePublishedVersion(
                yearEntity.getId(),
                yearEntity.getVersion(),
                draft.getId(),
                operatorId
        ) != 1) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
        }

        LOGGER.info(
                "event=work_calendar_published calendarYear={} newVersionId={} previousVersionId={} "
                        + "contentHash={} operatorId={} addedCount={} removedCount={} changedCount={}",
                draft.getCalendarYear(),
                draft.getId(),
                previousActiveVersionId,
                draft.getContentHash(),
                operatorId,
                publicationDiff.addedCount(),
                publicationDiff.removedCount(),
                publicationDiff.changedCount()
        );
        return getYearDetails(draft.getCalendarYear());
    }

    private WorkCalendarYearEntity getOrCreateYearForUpdate(int year) {
        ruleService.validateYear(year);
        yearMapper.insertIgnore(year, currentOperatorId());
        WorkCalendarYearEntity yearEntity = yearMapper.selectByCalendarYearForUpdate(year);
        if (yearEntity == null) {
            throw new IllegalStateException("工作日历年度入口创建失败: " + year);
        }
        return yearEntity;
    }

    private WorkCalendarYearEntity requireYearForUpdate(int year) {
        ruleService.validateYear(year);
        WorkCalendarYearEntity yearEntity = yearMapper.selectByCalendarYearForUpdate(year);
        if (yearEntity == null) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DRAFT_NOT_FOUND);
        }
        return yearEntity;
    }

    private WorkCalendarVersionEntity requireCurrentDraft(
            WorkCalendarYearEntity yearEntity,
            Long draftVersionId,
            Integer expectedLockVersion
    ) {
        if (draftVersionId == null
                || expectedLockVersion == null
                || !Objects.equals(yearEntity.getDraftVersionId(), draftVersionId)) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
        }
        WorkCalendarVersionEntity draft = requireDraft(draftVersionId, expectedLockVersion);
        if (!Objects.equals(draft.getCalendarYearId(), yearEntity.getId())) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
        }
        return draft;
    }

    private WorkCalendarVersionEntity requireDraft(Long draftVersionId, Integer expectedLockVersion) {
        if (draftVersionId == null || expectedLockVersion == null) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DRAFT_NOT_FOUND);
        }
        WorkCalendarVersionEntity draft = versionMapper.selectById(draftVersionId);
        if (draft == null || draft.getStatus() != WorkCalendarVersionStatus.DRAFT) {
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_DRAFT_NOT_FOUND);
        }
        if (!Objects.equals(draft.getVersion(), expectedLockVersion)) {
            LOGGER.warn(
                    "event=work_calendar_version_conflict draftVersionId={} expectedVersion={} actualVersion={}",
                    draftVersionId,
                    expectedLockVersion,
                    draft.getVersion()
            );
            throw new BizException(WorkCalendarErrorCode.WORK_CALENDAR_VERSION_CONFLICT);
        }
        return draft;
    }

    private Long currentOperatorId() {
        Long operatorId = OperatorContext.getOperatorId();
        return operatorId == null ? 0L : operatorId;
    }
}
