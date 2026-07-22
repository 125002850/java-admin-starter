package com.oigit.admin.mdm.export.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.mdm.export.enums.ExportCenterErrorCode;
import com.oigit.admin.mdm.export.enums.ExportDeleteReason;
import com.oigit.admin.mdm.export.enums.ExportRecordStatus;
import com.oigit.admin.mdm.export.infra.entity.ExportRecordEntity;
import com.oigit.admin.mdm.export.infra.mapper.ExportRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ExportRecordService {

    private final ExportRecordMapper exportRecordMapper;
    private final MybatisPlusQueryExecutor mybatisPlusQueryExecutor;

    public ExportRecordService(
            ExportRecordMapper exportRecordMapper,
            MybatisPlusQueryExecutor mybatisPlusQueryExecutor
    ) {
        this.exportRecordMapper = exportRecordMapper;
        this.mybatisPlusQueryExecutor = mybatisPlusQueryExecutor;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createProcessingRecord(ExportRecordEntity entity) {
        entity.setStatus(ExportRecordStatus.PROCESSING.getIntCode());
        if (entity.getDeleted() == null) {
            entity.setDeleted(0L);
        }
        exportRecordMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long recordId, String objectKey, String contentType, Long fileSize, String storageType) {
        markSuccessInternal(recordId, objectKey, contentType, fileSize, storageType);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markBatchSuccess(
            Long recordId,
            String objectKey,
            String contentType,
            Long fileSize,
            String storageType,
            List<Long> sourceRecordIds,
            Long operatorId
    ) {
        markSuccessInternal(recordId, objectKey, contentType, fileSize, storageType);
        for (Long sourceRecordId : sourceRecordIds) {
            recordDownloadLinkAcquiredInternal(sourceRecordId, operatorId);
        }
    }

    private void markSuccessInternal(
            Long recordId,
            String objectKey,
            String contentType,
            Long fileSize,
            String storageType
    ) {
        ExportRecordEntity entity = getActiveRequired(recordId);
        ensureStatus(entity, ExportRecordStatus.PROCESSING);
        entity.setStatus(ExportRecordStatus.SUCCESS.getIntCode());
        entity.setObjectKey(objectKey);
        entity.setContentType(contentType);
        entity.setFileSize(fileSize);
        entity.setStorageType(storageType);
        entity.setFinishedTime(LocalDateTime.now());
        exportRecordMapper.updateById(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long recordId, String failCode, String failMessage) {
        ExportRecordEntity entity = getActiveRequired(recordId);
        ensureStatus(entity, ExportRecordStatus.PROCESSING);
        entity.setStatus(ExportRecordStatus.FAILED.getIntCode());
        entity.setFailCode(failCode);
        entity.setFailMessage(failMessage);
        entity.setFinishedTime(LocalDateTime.now());
        exportRecordMapper.updateById(entity);
    }

    @Transactional
    public void markExpired(Long recordId) {
        ExportRecordEntity entity = getActiveRequired(recordId);
        ensureStatus(entity, ExportRecordStatus.SUCCESS);
        entity.setStatus(ExportRecordStatus.EXPIRED.getIntCode());
        exportRecordMapper.updateById(entity);
    }

    @Transactional
    public void markDeleted(Long recordId, ExportDeleteReason deleteReason) {
        ExportRecordEntity entity = getRequired(recordId);
        if (isDeleted(entity)) {
            return;
        }
        exportRecordMapper.update(
                null,
                Wrappers.<ExportRecordEntity>lambdaUpdate()
                        .set(ExportRecordEntity::getDeleted, currentDeletedTimestamp())
                        .set(ExportRecordEntity::getDeleteReason, deleteReason.getIntCode())
                        .set(ExportRecordEntity::getDeletedTime, LocalDateTime.now())
                        .eq(ExportRecordEntity::getId, recordId)
                        .eq(ExportRecordEntity::getDeleted, 0L)
        );
    }

    @Transactional(readOnly = true)
    public Page<ExportRecordEntity> pageMyRecords(
            QueryAst queryAst,
            SceneQueryDefinition<ExportRecordEntity> sceneQueryDefinition
    ) {
        return mybatisPlusQueryExecutor.selectPage(exportRecordMapper, queryAst, sceneQueryDefinition);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDownloadLinkAcquired(Long recordId, Long operatorId) {
        recordDownloadLinkAcquiredInternal(recordId, operatorId);
    }

    private void recordDownloadLinkAcquiredInternal(Long recordId, Long operatorId) {
        ExportRecordEntity entity = getActiveRequired(recordId);
        ensureStatus(entity, ExportRecordStatus.SUCCESS);
        int nextDownloadCount = entity.getDownloadCount() == null ? 1 : entity.getDownloadCount() + 1;
        entity.setDownloadCount(nextDownloadCount);
        entity.setLastDownloadTime(LocalDateTime.now());
        entity.setLastDownloadBy(operatorId);
        exportRecordMapper.updateById(entity);
    }

    public ExportRecordEntity getRequired(Long recordId) {
        ExportRecordEntity entity = exportRecordMapper.selectById(recordId);
        if (entity == null) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND);
        }
        return entity;
    }

    public ExportRecordEntity getActiveRequired(Long recordId) {
        ExportRecordEntity entity = getRequired(recordId);
        if (isDeleted(entity)) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND);
        }
        return entity;
    }

    @Transactional(readOnly = true)
    public List<ExportRecordEntity> listByIds(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return Collections.emptyList();
        }
        return exportRecordMapper.selectBatchIds(recordIds);
    }

    @Transactional(readOnly = true)
    public List<ExportRecordEntity> listActiveByIds(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return Collections.emptyList();
        }
        return exportRecordMapper.selectList(
                Wrappers.<ExportRecordEntity>lambdaQuery()
                        .in(ExportRecordEntity::getId, recordIds)
                        .eq(ExportRecordEntity::getDeleted, 0L)
        );
    }

    private void ensureStatus(ExportRecordEntity entity, ExportRecordStatus expectedStatus) {
        if (entity.getStatus() == null || !entity.getStatus().equals(expectedStatus.getIntCode())) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID);
        }
    }

    private boolean isDeleted(ExportRecordEntity entity) {
        return entity.getDeleted() != null && entity.getDeleted() != 0L;
    }

    private long currentDeletedTimestamp() {
        return Instant.now().getEpochSecond();
    }
}
