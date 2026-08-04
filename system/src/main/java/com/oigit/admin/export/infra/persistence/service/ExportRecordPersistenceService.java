package com.oigit.admin.export.infra.persistence.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.export.enums.ExportDeleteReason;
import com.oigit.admin.export.infra.persistence.entity.ExportRecordEntity;

import java.time.LocalDateTime;
import java.util.Collection;

public interface ExportRecordPersistenceService extends IService<ExportRecordEntity> {

    Page<ExportRecordEntity> pageBy(QueryAst queryAst);

    int maxQueryComplexityScore();

    int markSuccess(
            Long recordId,
            String objectKey,
            String contentType,
            Long fileSize,
            String storageType,
            LocalDateTime finishedTime
    );

    int markFailed(Long recordId, String failCode, String failMessage, LocalDateTime finishedTime);

    int markExpired(Long recordId);

    int markDeleted(Collection<Long> recordIds, ExportDeleteReason deleteReason, LocalDateTime deletedTime, long deletedValue);

    int recordDownloadLinksAcquired(Collection<Long> recordIds, Long operatorId, LocalDateTime downloadTime);
}
