package com.oigit.admin.export.domain.repository;

import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.export.domain.model.ExportRecord;
import com.oigit.admin.export.domain.model.ExportRecordPage;
import com.oigit.admin.export.enums.ExportDeleteReason;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExportRecordRepository {

    void create(ExportRecord record);

    Optional<ExportRecord> findActiveById(Long recordId);

    List<ExportRecord> findActiveByIds(Collection<Long> recordIds);

    ExportRecordPage page(QueryAst queryAst);

    int maxQueryComplexityScore();

    boolean markSuccess(
            Long recordId,
            String objectKey,
            String contentType,
            Long fileSize,
            String storageType,
            LocalDateTime finishedTime
    );

    boolean markFailed(Long recordId, String failCode, String failMessage, LocalDateTime finishedTime);

    boolean markExpired(Long recordId);

    int markDeleted(Collection<Long> recordIds, ExportDeleteReason deleteReason, LocalDateTime deletedTime, long deletedValue);

    boolean recordDownloadLinkAcquired(Long recordId, Long operatorId, LocalDateTime downloadTime);

    int recordDownloadLinksAcquired(Collection<Long> recordIds, Long operatorId, LocalDateTime downloadTime);
}
