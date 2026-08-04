package com.oigit.admin.export.app;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.export.domain.repository.ExportRecordRepository;
import com.oigit.admin.export.enums.ExportCenterErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportRecordLifecycleAppServiceTests {

    @Test
    void markSuccess_should_reject_lost_state_transition() {
        ExportRecordRepository repository = mock(ExportRecordRepository.class);
        when(repository.markSuccess(eq(1L), any(), any(), any(), any(), any(LocalDateTime.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> new ExportRecordLifecycleAppService(repository)
                .markSuccess(1L, "export/demo.csv", "text/csv", 10L, "local"))
                .isInstanceOf(BizException.class)
                .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID.getMsg());
    }

    @Test
    void markBatchSuccess_should_increment_all_sources_in_one_repository_call() {
        ExportRecordRepository repository = mock(ExportRecordRepository.class);
        when(repository.markSuccess(eq(10L), any(), any(), any(), any(), any(LocalDateTime.class)))
                .thenReturn(true);
        when(repository.recordDownloadLinksAcquired(eq(List.of(1L, 2L)), eq(99L), any(LocalDateTime.class)))
                .thenReturn(2);
        ExportRecordLifecycleAppService service = new ExportRecordLifecycleAppService(repository);

        service.markBatchSuccess(10L, "export/batch.zip", "application/zip", 20L, "local", List.of(1L, 2L), 99L);

        verify(repository).recordDownloadLinksAcquired(eq(List.of(1L, 2L)), eq(99L), any(LocalDateTime.class));
    }
}
