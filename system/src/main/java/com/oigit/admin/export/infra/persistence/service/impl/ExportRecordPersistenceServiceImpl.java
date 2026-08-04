package com.oigit.admin.export.infra.persistence.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.export.enums.ExportDeleteReason;
import com.oigit.admin.export.enums.ExportRecordStatus;
import com.oigit.admin.export.infra.persistence.entity.ExportRecordEntity;
import com.oigit.admin.export.infra.persistence.mapper.ExportRecordMapper;
import com.oigit.admin.export.infra.persistence.service.ExportRecordPersistenceService;
import com.oigit.admin.export.infra.query.ExportRecordSceneQueryDefinition;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;

@Service
public class ExportRecordPersistenceServiceImpl
        extends ServiceImpl<ExportRecordMapper, ExportRecordEntity>
        implements ExportRecordPersistenceService {

    private final MybatisPlusQueryExecutor queryExecutor;
    private final ExportRecordSceneQueryDefinition queryDefinition;

    public ExportRecordPersistenceServiceImpl(
            ExportRecordMapper exportRecordMapper,
            MybatisPlusQueryExecutor queryExecutor,
            ExportRecordSceneQueryDefinition queryDefinition
    ) {
        this.baseMapper = exportRecordMapper;
        this.queryExecutor = queryExecutor;
        this.queryDefinition = queryDefinition;
    }

    @Override
    public Page<ExportRecordEntity> pageBy(QueryAst queryAst) {
        return queryExecutor.selectPage(getBaseMapper(), queryAst, queryDefinition);
    }

    @Override
    public int maxQueryComplexityScore() {
        return queryDefinition.maxComplexityScore();
    }

    @Override
    public int markSuccess(
            Long recordId,
            String objectKey,
            String contentType,
            Long fileSize,
            String storageType,
            LocalDateTime finishedTime
    ) {
        return updateWithAudit(Wrappers.<ExportRecordEntity>lambdaUpdate()
                .set(ExportRecordEntity::getStatus, ExportRecordStatus.SUCCESS.getIntCode())
                .set(ExportRecordEntity::getObjectKey, objectKey)
                .set(ExportRecordEntity::getContentType, contentType)
                .set(ExportRecordEntity::getFileSize, fileSize)
                .set(ExportRecordEntity::getStorageType, storageType)
                .set(ExportRecordEntity::getFinishedTime, finishedTime)
                .eq(ExportRecordEntity::getId, recordId)
                .eq(ExportRecordEntity::getStatus, ExportRecordStatus.PROCESSING.getIntCode())
                .eq(ExportRecordEntity::getDeleted, 0L));
    }

    @Override
    public int markFailed(Long recordId, String failCode, String failMessage, LocalDateTime finishedTime) {
        return updateWithAudit(Wrappers.<ExportRecordEntity>lambdaUpdate()
                .set(ExportRecordEntity::getStatus, ExportRecordStatus.FAILED.getIntCode())
                .set(ExportRecordEntity::getFailCode, failCode)
                .set(ExportRecordEntity::getFailMessage, failMessage)
                .set(ExportRecordEntity::getFinishedTime, finishedTime)
                .eq(ExportRecordEntity::getId, recordId)
                .eq(ExportRecordEntity::getStatus, ExportRecordStatus.PROCESSING.getIntCode())
                .eq(ExportRecordEntity::getDeleted, 0L));
    }

    @Override
    public int markExpired(Long recordId) {
        return updateWithAudit(Wrappers.<ExportRecordEntity>lambdaUpdate()
                .set(ExportRecordEntity::getStatus, ExportRecordStatus.EXPIRED.getIntCode())
                .eq(ExportRecordEntity::getId, recordId)
                .eq(ExportRecordEntity::getStatus, ExportRecordStatus.SUCCESS.getIntCode())
                .eq(ExportRecordEntity::getDeleted, 0L));
    }

    @Override
    public int markDeleted(
            Collection<Long> recordIds,
            ExportDeleteReason deleteReason,
            LocalDateTime deletedTime,
            long deletedValue
    ) {
        if (recordIds == null || recordIds.isEmpty()) {
            return 0;
        }
        return updateWithAudit(Wrappers.<ExportRecordEntity>lambdaUpdate()
                .set(ExportRecordEntity::getDeleted, deletedValue)
                .set(ExportRecordEntity::getDeleteReason, deleteReason.getIntCode())
                .set(ExportRecordEntity::getDeletedTime, deletedTime)
                .in(ExportRecordEntity::getId, recordIds)
                .eq(ExportRecordEntity::getDeleted, 0L));
    }

    @Override
    public int recordDownloadLinksAcquired(
            Collection<Long> recordIds,
            Long operatorId,
            LocalDateTime downloadTime
    ) {
        if (recordIds == null || recordIds.isEmpty()) {
            return 0;
        }
        return updateWithAudit(Wrappers.<ExportRecordEntity>lambdaUpdate()
                .setSql("download_count = coalesce(download_count, 0) + 1")
                .set(ExportRecordEntity::getLastDownloadTime, downloadTime)
                .set(ExportRecordEntity::getLastDownloadBy, operatorId)
                .in(ExportRecordEntity::getId, recordIds)
                .eq(ExportRecordEntity::getStatus, ExportRecordStatus.SUCCESS.getIntCode())
                .eq(ExportRecordEntity::getDeleted, 0L));
    }

    private int updateWithAudit(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExportRecordEntity> update) {
        return getBaseMapper().update(new ExportRecordEntity(), update);
    }
}
