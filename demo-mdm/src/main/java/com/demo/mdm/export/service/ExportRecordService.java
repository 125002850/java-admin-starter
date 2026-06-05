package com.demo.mdm.export.service;

import com.demo.core.exception.BizException;
import com.demo.mdm.export.enums.ExportCenterErrorCode;
import com.demo.mdm.export.enums.ExportDeleteReason;
import com.demo.mdm.export.enums.ExportRecordStatus;
import com.demo.mdm.export.infra.entity.ExportRecordEntity;
import com.demo.mdm.export.infra.mapper.ExportRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ExportRecordService {

    private final ExportRecordMapper exportRecordMapper;

    public ExportRecordService(ExportRecordMapper exportRecordMapper) {
        this.exportRecordMapper = exportRecordMapper;
    }

    @Transactional
    public Long createProcessingRecord(ExportRecordEntity entity) {
        entity.setStatus(ExportRecordStatus.PROCESSING.getCode());
        if (entity.getDeleted() == null) {
            entity.setDeleted(0L);
        }
        exportRecordMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public void markSuccess(Long recordId, String objectKey, String contentType, Long fileSize, String storageType) {
        ExportRecordEntity entity = getRequired(recordId);
        ensureStatus(entity, ExportRecordStatus.PROCESSING);
        entity.setStatus(ExportRecordStatus.SUCCESS.getCode());
        entity.setObjectKey(objectKey);
        entity.setContentType(contentType);
        entity.setFileSize(fileSize);
        entity.setStorageType(storageType);
        entity.setFinishedTime(LocalDateTime.now());
        exportRecordMapper.updateById(entity);
    }

    @Transactional
    public void markFailed(Long recordId, String failCode, String failMessage) {
        ExportRecordEntity entity = getRequired(recordId);
        ensureStatus(entity, ExportRecordStatus.PROCESSING);
        entity.setStatus(ExportRecordStatus.FAILED.getCode());
        entity.setFailCode(failCode);
        entity.setFailMessage(failMessage);
        entity.setFinishedTime(LocalDateTime.now());
        exportRecordMapper.updateById(entity);
    }

    @Transactional
    public void markExpired(Long recordId) {
        ExportRecordEntity entity = getRequired(recordId);
        ensureStatus(entity, ExportRecordStatus.SUCCESS);
        entity.setStatus(ExportRecordStatus.EXPIRED.getCode());
        exportRecordMapper.updateById(entity);
    }

    @Transactional
    public void markDeleted(Long recordId, ExportDeleteReason deleteReason) {
        ExportRecordEntity entity = getRequired(recordId);
        if (!isDeletable(entity.getStatus())) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID);
        }
        entity.setStatus(ExportRecordStatus.DELETED.getCode());
        entity.setDeleteReason(deleteReason.getCode());
        entity.setDeletedTime(LocalDateTime.now());
        exportRecordMapper.updateById(entity);
    }

    public ExportRecordEntity getRequired(Long recordId) {
        ExportRecordEntity entity = exportRecordMapper.selectById(recordId);
        if (entity == null) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND);
        }
        return entity;
    }

    private void ensureStatus(ExportRecordEntity entity, ExportRecordStatus expectedStatus) {
        if (entity.getStatus() == null || entity.getStatus() != expectedStatus.getCode()) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID);
        }
    }

    private boolean isDeletable(Integer status) {
        return status != null
            && (status == ExportRecordStatus.SUCCESS.getCode() || status == ExportRecordStatus.EXPIRED.getCode());
    }
}
