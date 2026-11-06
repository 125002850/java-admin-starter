package com.oigit.admin.workcalendar;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.workcalendar.domain.service.WorkCalendarRuleService;
import com.oigit.admin.workcalendar.enums.*;
import com.oigit.admin.workcalendar.infra.persistence.entity.*;
import com.oigit.admin.workcalendar.infra.persistence.mapper.*;
import com.oigit.admin.workcalendar.infra.persistence.repository.MybatisWorkCalendarRepository;
import com.oigit.admin.workcalendar.infra.serialization.JacksonWorkCalendarCodec;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VersionEvaluationTests {
    @Test void evaluatesSavedDraftActiveAndHistoricalSnapshotsWithVersionProtection() {
        var years = mock(WorkCalendarYearMapper.class);
        var versions = mock(WorkCalendarVersionMapper.class);
        var overrides = mock(WorkCalendarDateOverrideMapper.class);
        var codec = mock(JacksonWorkCalendarCodec.class);
        when(codec.decodePeriods(any())).thenReturn(List.of());
        when(overrides.selectList(any())).thenReturn(List.of());
        var repo = new MybatisWorkCalendarRepository(years, versions, overrides, new WorkCalendarRuleService(), codec);
        var version = new WorkCalendarVersionEntity();
        version.setId(1L); version.setCalendarYear(2026); version.setVersion(2); version.setVersionNo(1);
        version.setStatus(WorkCalendarVersionStatus.DRAFT);
        when(versions.selectById(1L)).thenReturn(version);
        var year = new WorkCalendarYearEntity(); year.setActiveVersionId(1L);
        when(years.selectOne(any())).thenReturn(year);
        var time = LocalDateTime.of(2026, 11, 6, 10, 0);
        var draft = repo.evaluateVersion(1L, 2, time);
        assertThat(draft.basis()).isEqualTo(ClassificationBasis.SAVED_DRAFT);
        assertThat(draft.holidayStatus()).isEqualTo(HolidayStatus.CANDIDATE);
        assertThat(draft.authoritative()).isFalse();
        version.setStatus(WorkCalendarVersionStatus.PUBLISHED);
        assertThat(repo.evaluateVersion(1L, 2, time).authoritative()).isTrue();
        year.setActiveVersionId(3L);
        var history = repo.evaluateVersion(1L, 2, time);
        assertThat(history.authoritative()).isFalse();
        assertThat(history.basis()).isEqualTo(ClassificationBasis.PUBLISHED_SNAPSHOT);
        assertThat(history.holidayStatus()).isEqualTo(HolidayStatus.CONFIRMED);
        assertThatThrownBy(() -> repo.evaluateVersion(1L, 1, time)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> repo.evaluateVersion(1L, 2, time.plusYears(1))).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> repo.evaluateVersion(99L, 2, time)).isInstanceOf(BizException.class);
    }
}
