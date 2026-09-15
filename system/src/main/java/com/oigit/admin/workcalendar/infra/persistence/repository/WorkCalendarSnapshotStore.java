package com.oigit.admin.workcalendar.infra.persistence.repository;

import com.oigit.admin.workcalendar.infra.serialization.JacksonWorkCalendarCodec;
import com.oigit.admin.workcalendar.domain.service.WorkCalendarRuleService;
import com.oigit.admin.workcalendar.domain.service.WorkCalendarPublicationPolicy;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oigit.admin.workcalendar.enums.WorkCalendarVersionStatus;
import com.oigit.admin.workcalendar.infra.persistence.entity.WorkCalendarDateOverrideEntity;
import com.oigit.admin.workcalendar.infra.persistence.entity.WorkCalendarVersionEntity;
import com.oigit.admin.workcalendar.infra.persistence.entity.WorkCalendarYearEntity;
import com.oigit.admin.workcalendar.infra.persistence.mapper.WorkCalendarDateOverrideMapper;
import com.oigit.admin.workcalendar.infra.persistence.mapper.WorkCalendarVersionMapper;
import com.oigit.admin.workcalendar.infra.persistence.mapper.WorkCalendarYearMapper;
import com.oigit.admin.workcalendar.domain.model.CalendarDateOverride;
import com.oigit.admin.workcalendar.domain.model.CalendarVersionSnapshot;
import com.oigit.admin.workcalendar.domain.model.WorkingPeriod;
import java.util.List;

final class WorkCalendarSnapshotStore {

    private final WorkCalendarYearMapper yearMapper;
    private final WorkCalendarVersionMapper versionMapper;
    private final WorkCalendarDateOverrideMapper dateOverrideMapper;
    private final JacksonWorkCalendarCodec jsonCodec;

    WorkCalendarSnapshotStore(
            WorkCalendarYearMapper yearMapper,
            WorkCalendarVersionMapper versionMapper,
            WorkCalendarDateOverrideMapper dateOverrideMapper,
            JacksonWorkCalendarCodec jsonCodec
    ) {
        this.yearMapper = yearMapper;
        this.versionMapper = versionMapper;
        this.dateOverrideMapper = dateOverrideMapper;
        this.jsonCodec = jsonCodec;
    }

    WorkCalendarYearEntity findYear(int year) {
        return yearMapper.selectOne(
                Wrappers.<WorkCalendarYearEntity>lambdaQuery()
                        .eq(WorkCalendarYearEntity::getCalendarYear, year)
                        .last("limit 1")
        );
    }

    CalendarVersionSnapshot loadLatestPublishedSnapshot() {
        WorkCalendarVersionEntity version = versionMapper.selectOne(
                Wrappers.<WorkCalendarVersionEntity>lambdaQuery()
                        .eq(WorkCalendarVersionEntity::getStatus, WorkCalendarVersionStatus.PUBLISHED)
                        .orderByDesc(WorkCalendarVersionEntity::getPublishedAt)
                        .orderByDesc(WorkCalendarVersionEntity::getId)
                        .last("limit 1")
        );
        return version == null ? null : loadSnapshot(version);
    }

    CalendarVersionSnapshot loadSnapshotIfPresent(Long versionId) {
        if (versionId == null) {
            return null;
        }
        WorkCalendarVersionEntity version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new IllegalStateException("工作日历版本指针无效: " + versionId);
        }
        return loadSnapshot(version);
    }

    CalendarVersionSnapshot loadSnapshot(WorkCalendarVersionEntity version) {
        List<WorkingPeriod> periods = jsonCodec.decodePeriods(version.getStandardPeriodsJson());
        List<CalendarDateOverride> overrides = dateOverrideMapper.selectList(
                        Wrappers.<WorkCalendarDateOverrideEntity>lambdaQuery()
                                .eq(WorkCalendarDateOverrideEntity::getCalendarVersionId, version.getId())
                                .orderByAsc(WorkCalendarDateOverrideEntity::getCalendarDate)
                ).stream()
                .map(this::toDomain)
                .toList();
        return new CalendarVersionSnapshot(toVersion(version), periods, overrides);
    }

    void replaceDateOverrides(Long versionId, List<CalendarDateOverride> overrides) {
        dateOverrideMapper.physicalDeleteByVersionId(versionId);
        for (CalendarDateOverride override : overrides) {
            WorkCalendarDateOverrideEntity entity = new WorkCalendarDateOverrideEntity();
            entity.setCalendarVersionId(versionId);
            entity.setCalendarDate(override.date());
            entity.setOverrideType(override.type());
            entity.setDateName(override.name());
            entity.setCustomPeriodsJson(override.customPeriods() == null
                    ? null
                    : jsonCodec.encodePeriods(override.customPeriods()));
            entity.setSourceNote(override.sourceNote());
            dateOverrideMapper.insert(entity);
        }
    }

    int nextVersionNo(Long yearId) {
        WorkCalendarVersionEntity latest = versionMapper.selectOne(
                Wrappers.<WorkCalendarVersionEntity>lambdaQuery()
                        .eq(WorkCalendarVersionEntity::getCalendarYearId, yearId)
                        .orderByDesc(WorkCalendarVersionEntity::getVersionNo)
                        .last("limit 1")
        );
        return latest == null ? 1 : latest.getVersionNo() + 1;
    }

    private CalendarDateOverride toDomain(WorkCalendarDateOverrideEntity entity) {
        return new CalendarDateOverride(
                entity.getCalendarDate(),
                entity.getOverrideType(),
                entity.getDateName(),
                entity.getCustomPeriodsJson() == null
                        ? null
                        : jsonCodec.decodePeriods(entity.getCustomPeriodsJson()),
                entity.getSourceNote()
        );
    }

    private static com.oigit.admin.workcalendar.domain.model.CalendarVersion toVersion(WorkCalendarVersionEntity entity) {
        return new com.oigit.admin.workcalendar.domain.model.CalendarVersion(entity.getId(), entity.getCalendarYear(), entity.getVersionNo(), entity.getStatus(), entity.getVersion(), entity.getContentHash(), entity.getPublishedBy(), entity.getPublishedAt());
    }
}
