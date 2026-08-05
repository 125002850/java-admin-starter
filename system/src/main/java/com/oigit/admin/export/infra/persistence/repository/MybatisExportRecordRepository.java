package com.oigit.admin.export.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.export.domain.model.ExportRecord;
import com.oigit.admin.export.domain.model.ExportRecordPage;
import com.oigit.admin.export.domain.repository.ExportRecordRepository;
import com.oigit.admin.export.enums.ExportDeleteReason;
import com.oigit.admin.export.enums.ExportRecordStatus;
import com.oigit.admin.export.infra.persistence.entity.ExportRecordEntity;
import com.oigit.admin.export.infra.persistence.service.ExportRecordPersistenceService;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisExportRecordRepository implements ExportRecordRepository {

    private final ExportRecordPersistenceService persistenceService;

    public MybatisExportRecordRepository(ExportRecordPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public void create(ExportRecord record) {
        ExportRecordEntity entity = toEntity(record);
        persistenceService.save(entity);
        copyPersistenceState(entity, record);
    }

    @Override
    public Optional<ExportRecord> findActiveById(Long recordId) {
        return Optional.ofNullable(persistenceService.getById(recordId)).map(this::toDomain);
    }

    @Override
    public List<ExportRecord> findActiveByIds(Collection<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return List.of();
        }
        return persistenceService.list(Wrappers.<ExportRecordEntity>lambdaQuery()
                        .in(ExportRecordEntity::getId, recordIds)
                        .eq(ExportRecordEntity::getDeleted, 0L))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ExportRecordPage page(QueryAst queryAst) {
        Page<ExportRecordEntity> page = persistenceService.pageBy(queryAst);
        return new ExportRecordPage(page.getRecords().stream().map(this::toDomain).toList(), page.getTotal());
    }

    @Override
    public int maxQueryComplexityScore() {
        return persistenceService.maxQueryComplexityScore();
    }

    @Override
    public boolean markSuccess(
            Long recordId,
            String objectKey,
            String contentType,
            Long fileSize,
            String storageType,
            LocalDateTime finishedTime
    ) {
        return persistenceService.markSuccess(
                recordId,
                objectKey,
                contentType,
                fileSize,
                storageType,
                finishedTime
        ) == 1;
    }

    @Override
    public boolean markFailed(Long recordId, String failCode, String failMessage, LocalDateTime finishedTime) {
        return persistenceService.markFailed(recordId, failCode, failMessage, finishedTime) == 1;
    }

    @Override
    public boolean markExpired(Long recordId) {
        return persistenceService.markExpired(recordId) == 1;
    }

    @Override
    public int markDeleted(
            Collection<Long> recordIds,
            ExportDeleteReason deleteReason,
            LocalDateTime deletedTime,
            long deletedValue
    ) {
        return persistenceService.markDeleted(recordIds, deleteReason, deletedTime, deletedValue);
    }

    @Override
    public boolean recordDownloadLinkAcquired(Long recordId, Long operatorId, LocalDateTime downloadTime) {
        return persistenceService.recordDownloadLinksAcquired(List.of(recordId), operatorId, downloadTime) == 1;
    }

    @Override
    public int recordDownloadLinksAcquired(Collection<Long> recordIds, Long operatorId, LocalDateTime downloadTime) {
        return persistenceService.recordDownloadLinksAcquired(recordIds, operatorId, downloadTime);
    }

    private ExportRecordEntity toEntity(ExportRecord record) {
        ExportRecordEntity entity = new ExportRecordEntity();
        entity.setId(record.getId());
        entity.setExportBizCode(record.getExportBizCode());
        entity.setExportBizName(record.getExportBizName());
        entity.setFileName(record.getFileName());
        entity.setFileType(record.getFileType());
        entity.setContentType(record.getContentType());
        entity.setFileSize(record.getFileSize());
        entity.setObjectKey(record.getObjectKey());
        entity.setStorageType(record.getStorageType());
        entity.setStatus(record.getStatus() == null ? null : record.getStatus().getIntCode());
        entity.setFinishedTime(record.getFinishedTime());
        entity.setExpireTime(record.getExpireTime());
        entity.setDeletedTime(record.getDeletedTime());
        entity.setDeleteReason(record.getDeleteReason());
        entity.setFailCode(record.getFailCode());
        entity.setFailMessage(record.getFailMessage());
        entity.setQuerySnapshotJson(record.getQuerySnapshotJson());
        entity.setQuerySnapshotSummary(record.getQuerySnapshotSummary());
        entity.setDownloadCount(record.getDownloadCount());
        entity.setLastDownloadTime(record.getLastDownloadTime());
        entity.setLastDownloadBy(record.getLastDownloadBy());
        entity.setExpireSeconds(record.getExpireSeconds());
        entity.setVersion(record.getVersion());
        entity.setDeleted(record.getDeleted());
        return entity;
    }

    private ExportRecord toDomain(ExportRecordEntity entity) {
        ExportRecord record = new ExportRecord();
        record.setId(entity.getId());
        record.setExportBizCode(entity.getExportBizCode());
        record.setExportBizName(entity.getExportBizName());
        record.setFileName(entity.getFileName());
        record.setFileType(entity.getFileType());
        record.setContentType(entity.getContentType());
        record.setFileSize(entity.getFileSize());
        record.setObjectKey(entity.getObjectKey());
        record.setStorageType(entity.getStorageType());
        record.setStatus(ExportRecordStatus.fromCode(entity.getStatus() == null ? null : String.valueOf(entity.getStatus())));
        record.setFinishedTime(entity.getFinishedTime());
        record.setExpireTime(entity.getExpireTime());
        record.setDeletedTime(entity.getDeletedTime());
        record.setDeleteReason(entity.getDeleteReason());
        record.setFailCode(entity.getFailCode());
        record.setFailMessage(entity.getFailMessage());
        record.setQuerySnapshotJson(entity.getQuerySnapshotJson());
        record.setQuerySnapshotSummary(entity.getQuerySnapshotSummary());
        record.setDownloadCount(entity.getDownloadCount());
        record.setLastDownloadTime(entity.getLastDownloadTime());
        record.setLastDownloadBy(entity.getLastDownloadBy());
        record.setExpireSeconds(entity.getExpireSeconds());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        record.setCreateBy(entity.getCreateBy());
        record.setUpdateBy(entity.getUpdateBy());
        record.setDeleted(entity.getDeleted());
        record.setVersion(entity.getVersion());
        return record;
    }

    private void copyPersistenceState(ExportRecordEntity source, ExportRecord target) {
        target.setId(source.getId());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        target.setVersion(source.getVersion());
        target.setDeleted(source.getDeleted());
    }
}
