package com.demo.mdm.export;

import com.demo.core.exception.BizException;
import com.demo.mdm.export.enums.ExportCenterErrorCode;
import com.demo.mdm.export.enums.ExportDeleteReason;
import com.demo.mdm.export.enums.ExportRecordStatus;
import com.demo.mdm.export.infra.entity.ExportRecordEntity;
import com.demo.mdm.export.infra.mapper.ExportRecordMapper;
import com.demo.mdm.export.service.ExportRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportRecordServiceTests {

    @Mock
    private ExportRecordMapper exportRecordMapper;

    private ExportRecordService exportRecordService;

    @BeforeEach
    void setUp() {
        exportRecordService = new ExportRecordService(exportRecordMapper);
    }

    @Test
    void createProcessingRecord_should_initialize_processing_status() {
        ExportRecordEntity entity = buildRecord(1L, ExportRecordStatus.SUCCESS);

        Long recordId = exportRecordService.createProcessingRecord(entity);

        assertThat(recordId).isEqualTo(1L);
        assertThat(entity.getStatus()).isEqualTo(ExportRecordStatus.PROCESSING.getCode());
        assertThat(entity.getDeleted()).isEqualTo(0L);
        verify(exportRecordMapper).insert(entity);
    }

    @Test
    void markSuccess_should_reject_non_processing_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.SUCCESS));

        assertThatThrownBy(() -> exportRecordService.markSuccess(
            1L,
            "export/test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            1024L,
            "local"
        ))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID.getMsg());
    }

    @Test
    void markSuccess_should_persist_file_metadata() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.PROCESSING));

        exportRecordService.markSuccess(
            1L,
            "export/test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            1024L,
            "local"
        );

        ArgumentCaptor<ExportRecordEntity> captor = ArgumentCaptor.forClass(ExportRecordEntity.class);
        verify(exportRecordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExportRecordStatus.SUCCESS.getCode());
        assertThat(captor.getValue().getObjectKey()).isEqualTo("export/test.xlsx");
        assertThat(captor.getValue().getContentType())
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(captor.getValue().getFileSize()).isEqualTo(1024L);
        assertThat(captor.getValue().getStorageType()).isEqualTo("local");
        assertThat(captor.getValue().getFinishedTime()).isNotNull();
    }

    @Test
    void markFailed_should_reject_non_processing_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.SUCCESS));

        assertThatThrownBy(() -> exportRecordService.markFailed(1L, "EXPORT_FAILED", "导出失败"))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID.getMsg());
    }

    @Test
    void markExpired_should_allow_success_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.SUCCESS));

        exportRecordService.markExpired(1L);

        ArgumentCaptor<ExportRecordEntity> captor = ArgumentCaptor.forClass(ExportRecordEntity.class);
        verify(exportRecordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExportRecordStatus.EXPIRED.getCode());
    }

    @Test
    void markExpired_should_reject_failed_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.FAILED));

        assertThatThrownBy(() -> exportRecordService.markExpired(1L))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID.getMsg());
    }

    @Test
    void markDeleted_should_allow_success_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.SUCCESS));

        exportRecordService.markDeleted(1L, ExportDeleteReason.MANUAL);

        ArgumentCaptor<ExportRecordEntity> captor = ArgumentCaptor.forClass(ExportRecordEntity.class);
        verify(exportRecordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExportRecordStatus.DELETED.getCode());
        assertThat(captor.getValue().getDeleteReason()).isEqualTo(ExportDeleteReason.MANUAL.getCode());
        assertThat(captor.getValue().getDeletedTime()).isNotNull();
    }

    @Test
    void markDeleted_should_allow_expired_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.EXPIRED));

        exportRecordService.markDeleted(1L, ExportDeleteReason.EXPIRED_CLEANUP);

        ArgumentCaptor<ExportRecordEntity> captor = ArgumentCaptor.forClass(ExportRecordEntity.class);
        verify(exportRecordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExportRecordStatus.DELETED.getCode());
        assertThat(captor.getValue().getDeleteReason()).isEqualTo(ExportDeleteReason.EXPIRED_CLEANUP.getCode());
        assertThat(captor.getValue().getDeletedTime()).isNotNull();
    }

    @Test
    void markDeleted_should_reject_processing_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.PROCESSING));

        assertThatThrownBy(() -> exportRecordService.markDeleted(1L, ExportDeleteReason.MANUAL))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID.getMsg());
    }

    @Test
    void markDeleted_should_reject_failed_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.FAILED));

        assertThatThrownBy(() -> exportRecordService.markDeleted(1L, ExportDeleteReason.MANUAL))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID.getMsg());
    }

    @Test
    void getRequired_should_throw_when_record_missing() {
        when(exportRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> exportRecordService.getRequired(1L))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND.getMsg());
    }

    private ExportRecordEntity buildRecord(Long id, ExportRecordStatus status) {
        ExportRecordEntity entity = new ExportRecordEntity();
        entity.setId(id);
        entity.setExportBizCode("demo.export");
        entity.setExportBizName("演示导出");
        entity.setFileName("demo.xlsx");
        entity.setFileType("EXCEL");
        entity.setStatus(status.getCode());
        entity.setExpireTime(LocalDateTime.now().plusDays(1));
        entity.setExpireSeconds(3600);
        entity.setQuerySnapshotJson("{\"keyword\":\"demo\"}");
        entity.setQuerySnapshotSummary("keyword=demo");
        entity.setDeleted(0L);
        return entity;
    }
}
