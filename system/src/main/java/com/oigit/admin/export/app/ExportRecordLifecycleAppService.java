package com.oigit.admin.export.app;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.export.domain.model.ExportRecord;
import com.oigit.admin.export.domain.repository.ExportRecordRepository;
import com.oigit.admin.export.enums.ExportCenterErrorCode;
import com.oigit.admin.export.enums.ExportDeleteReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 导出记录短事务边界。渲染、对象存储和临时 URL 等外部调用不得进入这些事务。
 */
@Service
public class ExportRecordLifecycleAppService {

    private final ExportRecordRepository exportRecordRepository;

    public ExportRecordLifecycleAppService(ExportRecordRepository exportRecordRepository) {
        this.exportRecordRepository = exportRecordRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExportRecord createProcessingRecord(ExportRecord record) {
        record.initializeProcessing();
        exportRecordRepository.create(record);
        return record;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long recordId, String objectKey, String contentType, Long fileSize, String storageType) {
        boolean updated = exportRecordRepository.markSuccess(
                recordId,
                objectKey,
                contentType,
                fileSize,
                storageType,
                LocalDateTime.now()
        );
        requireValidTransition(updated);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markBatchSuccess(
            Long recordId,
            String objectKey,
            String contentType,
            Long fileSize,
            String storageType,
            Collection<Long> sourceRecordIds,
            Long operatorId
    ) {
        boolean updated = exportRecordRepository.markSuccess(
                recordId,
                objectKey,
                contentType,
                fileSize,
                storageType,
                LocalDateTime.now()
        );
        requireValidTransition(updated);
        int affected = exportRecordRepository.recordDownloadLinksAcquired(
                sourceRecordIds,
                operatorId,
                LocalDateTime.now()
        );
        if (affected != sourceRecordIds.size()) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long recordId, String failCode, String failMessage) {
        requireValidTransition(exportRecordRepository.markFailed(
                recordId,
                failCode,
                failMessage,
                LocalDateTime.now()
        ));
    }

    @Transactional
    public void markExpired(Long recordId) {
        requireValidTransition(exportRecordRepository.markExpired(recordId));
    }

    @Transactional
    public void markDeleted(Collection<Long> recordIds, ExportDeleteReason deleteReason) {
        int affected = exportRecordRepository.markDeleted(
                recordIds,
                deleteReason,
                LocalDateTime.now(),
                Instant.now().getEpochSecond()
        );
        if (affected != recordIds.size()) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDownloadLinkAcquired(Long recordId, Long operatorId) {
        requireValidTransition(exportRecordRepository.recordDownloadLinkAcquired(
                recordId,
                operatorId,
                LocalDateTime.now()
        ));
    }

    @Transactional(readOnly = true)
    public ExportRecord getActiveRequired(Long recordId) {
        return exportRecordRepository.findActiveById(recordId)
                .orElseThrow(() -> new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<ExportRecord> listActiveByIds(Collection<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return List.of();
        }
        return exportRecordRepository.findActiveByIds(recordIds);
    }

    private void requireValidTransition(boolean updated) {
        if (!updated) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID);
        }
    }
}
