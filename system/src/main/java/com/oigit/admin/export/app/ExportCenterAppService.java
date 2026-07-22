package com.oigit.admin.export.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.exception.CommonErrorCode;
import com.oigit.admin.core.export.model.ExportTaskResult;
import com.oigit.admin.core.export.spi.ExportTaskSubmitter;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.core.operator.OperatorUsernameResolver;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.support.DynamicQueryGuard;
import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.export.controller.dto.ExportBatchDownloadRspDTO;
import com.oigit.admin.export.controller.dto.ExportDownloadRspDTO;
import com.oigit.admin.export.controller.dto.ExportBatchDownloadReqDTO;
import com.oigit.admin.export.controller.dto.ExportRecordRspDTO;
import com.oigit.admin.export.controller.dto.ExportSubmitRspDTO;
import com.oigit.admin.export.controller.dto.ExportSubmitReqDTO;
import com.oigit.admin.export.controller.dto.query.ExportRecordDynamicPageReqDTO;
import com.oigit.admin.export.enums.ExportCenterErrorCode;
import com.oigit.admin.export.enums.ExportDeleteReason;
import com.oigit.admin.export.enums.ExportRecordStatus;
import com.oigit.admin.export.infra.entity.ExportRecordEntity;
import com.oigit.admin.export.query.ExportRecordSceneQueryDefinition;
import com.oigit.admin.export.query.ExportRecordSceneQueryMapper;
import com.oigit.admin.export.service.ExportDownloadService;
import com.oigit.admin.export.service.ExportBatchDownloadService;
import com.oigit.admin.export.service.ExportExecutionService;
import com.oigit.admin.export.service.ExportRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExportCenterAppService implements ExportTaskSubmitter {

    private static final Long FALLBACK_OPERATOR_ID = 0L;

    private final ExportExecutionService exportExecutionService;
    private final ExportDownloadService exportDownloadService;
    private final ExportBatchDownloadService exportBatchDownloadService;
    private final ExportRecordService exportRecordService;
    private final DynamicQueryGuard dynamicQueryGuard;
    private final ExportRecordSceneQueryMapper exportRecordSceneQueryMapper;
    private final ExportRecordSceneQueryDefinition exportRecordSceneQueryDefinition;
    private final OperatorUsernameResolver operatorUsernameResolver;

    public ExportCenterAppService(
            ExportExecutionService exportExecutionService,
            ExportDownloadService exportDownloadService,
            ExportBatchDownloadService exportBatchDownloadService,
            ExportRecordService exportRecordService,
            DynamicQueryGuard dynamicQueryGuard,
            ExportRecordSceneQueryMapper exportRecordSceneQueryMapper,
            ExportRecordSceneQueryDefinition exportRecordSceneQueryDefinition,
            OperatorUsernameResolver operatorUsernameResolver
    ) {
        this.exportExecutionService = exportExecutionService;
        this.exportDownloadService = exportDownloadService;
        this.exportBatchDownloadService = exportBatchDownloadService;
        this.exportRecordService = exportRecordService;
        this.dynamicQueryGuard = dynamicQueryGuard;
        this.exportRecordSceneQueryMapper = exportRecordSceneQueryMapper;
        this.exportRecordSceneQueryDefinition = exportRecordSceneQueryDefinition;
        this.operatorUsernameResolver = operatorUsernameResolver;
    }

    @Transactional
    public ExportSubmitRspDTO submit(ExportSubmitReqDTO reqDTO) {
        ExportRecordEntity entity = exportExecutionService.execute(reqDTO.getSceneCode(), reqDTO.getQuery());
        DownloadLinkIssuedResult downloadLinkIssuedResult = issueDownloadLink(entity);
        ExportRecordEntity record = downloadLinkIssuedResult.record();
        return toSubmitRsp(record, downloadLinkIssuedResult.downloadUrl(), auditUsernames(List.of(record)));
    }

    @Override
    @Transactional
    public ExportTaskResult submit(String sceneCode, com.fasterxml.jackson.databind.JsonNode query) {
        ExportRecordEntity entity = exportExecutionService.execute(sceneCode, query);
        DownloadLinkIssuedResult downloadLinkIssuedResult = issueDownloadLink(entity);
        return toTaskResult(downloadLinkIssuedResult.record(), downloadLinkIssuedResult.downloadUrl());
    }

    @Transactional(readOnly = true)
    public PageResult<ExportRecordRspDTO> pageMyExports(ExportRecordDynamicPageReqDTO reqDTO) {
        QueryAst queryAst = exportRecordSceneQueryMapper.map(reqDTO, currentOperatorId());
        dynamicQueryGuard.validate(queryAst, exportRecordSceneQueryDefinition.maxComplexityScore());
        Page<ExportRecordEntity> page = exportRecordService.pageMyRecords(queryAst, exportRecordSceneQueryDefinition);
        Map<Long, String> usernames = auditUsernames(page.getRecords());
        List<ExportRecordRspDTO> records = page.getRecords().stream()
                .map(entity -> toRecordRsp(entity, usernames))
                .toList();
        return new PageResult<>(records, page.getTotal());
    }

    @Transactional(readOnly = true)
    public ExportRecordRspDTO detail(Long recordId) {
        ExportRecordEntity entity = exportRecordService.getActiveRequired(recordId);
        ensureOwnedByCurrentOperator(entity);
        return toRecordRsp(entity, auditUsernames(List.of(entity)));
    }

    @Transactional
    public ExportDownloadRspDTO download(Long recordId) {
        ExportRecordEntity entity = exportRecordService.getActiveRequired(recordId);
        ensureOwnedByCurrentOperator(entity);
        String downloadUrl = exportDownloadService.fetchDownloadUrl(entity, currentOperatorId());
        ExportDownloadRspDTO rspDTO = new ExportDownloadRspDTO();
        rspDTO.setRecordId(entity.getId());
        rspDTO.setFileName(entity.getFileName());
        rspDTO.setDownloadUrl(downloadUrl);
        return rspDTO;
    }

    @Transactional
    public ExportBatchDownloadRspDTO batchDownload(ExportBatchDownloadReqDTO reqDTO) {
        ExportBatchDownloadService.BatchDownloadedFile file =
                exportBatchDownloadService.packageRecords(reqDTO.getIds(), currentOperatorId());
        ExportBatchDownloadRspDTO rspDTO = new ExportBatchDownloadRspDTO();
        rspDTO.setFileName(file.fileName());
        rspDTO.setDownloadUrl(file.downloadUrl());
        rspDTO.setContentType(file.contentType());
        rspDTO.setFileSize(file.fileSize());
        return rspDTO;
    }

    @Transactional
    public void delete(List<Long> recordIds) {
        List<Long> normalizedRecordIds = normalizeRecordIds(recordIds);
        List<ExportRecordEntity> records = listRequiredRecords(normalizedRecordIds);
        for (ExportRecordEntity record : records) {
            ensureOwnedByCurrentOperator(record);
        }
        for (Long recordId : normalizedRecordIds) {
            exportRecordService.markDeleted(recordId, ExportDeleteReason.MANUAL);
        }
    }

    private List<Long> normalizeRecordIds(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }
        Set<Long> distinctIds = new LinkedHashSet<>();
        for (Long recordId : recordIds) {
            if (recordId == null) {
                throw new BizException(CommonErrorCode.PARAM_ERROR);
            }
            distinctIds.add(recordId);
        }
        if (distinctIds.isEmpty()) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }
        return new ArrayList<>(distinctIds);
    }

    private List<ExportRecordEntity> listRequiredRecords(List<Long> recordIds) {
        List<ExportRecordEntity> records = exportRecordService.listByIds(recordIds);
        if (records.size() != recordIds.size()) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND);
        }
        Map<Long, ExportRecordEntity> recordMap = new LinkedHashMap<>();
        for (ExportRecordEntity record : records) {
            recordMap.put(record.getId(), record);
        }
        List<ExportRecordEntity> orderedRecords = new ArrayList<>();
        for (Long recordId : recordIds) {
            ExportRecordEntity record = recordMap.get(recordId);
            if (record == null) {
                throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND);
            }
            orderedRecords.add(record);
        }
        return orderedRecords;
    }

    private void ensureOwnedByCurrentOperator(ExportRecordEntity entity) {
        Long currentOperatorId = currentOperatorId();
        Long ownerId = entity.getCreateBy() == null ? FALLBACK_OPERATOR_ID : entity.getCreateBy();
        if (!ownerId.equals(currentOperatorId)) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_FORBIDDEN);
        }
    }

    private Long currentOperatorId() {
        Long operatorId = OperatorContext.getOperatorId();
        return operatorId == null ? FALLBACK_OPERATOR_ID : operatorId;
    }

    private ExportRecordRspDTO toRecordRsp(ExportRecordEntity entity, Map<Long, String> usernames) {
        ExportRecordStatus status = ExportRecordStatus.fromCode(String.valueOf(entity.getStatus()));
        ExportRecordRspDTO rspDTO = new ExportRecordRspDTO();
        rspDTO.setRecordId(entity.getId());
        rspDTO.setExportBizCode(entity.getExportBizCode());
        rspDTO.setExportBizName(entity.getExportBizName());
        rspDTO.setFileName(entity.getFileName());
        rspDTO.setFileType(entity.getFileType());
        rspDTO.setStatus(entity.getStatus());
        rspDTO.setStatusName(status == null ? null : status.name());
        rspDTO.setContentType(entity.getContentType());
        rspDTO.setFileSize(entity.getFileSize());
        rspDTO.setDownloadCount(entity.getDownloadCount());
        rspDTO.setQuerySnapshotSummary(entity.getQuerySnapshotSummary());
        rspDTO.setFinishedTime(entity.getFinishedTime());
        rspDTO.setExpireTime(entity.getExpireTime());
        rspDTO.setCreateTime(entity.getCreateTime());
        rspDTO.setCreateBy(auditUsername(usernames, entity.getCreateBy()));
        return rspDTO;
    }

    private ExportSubmitRspDTO toSubmitRsp(
            ExportRecordEntity entity,
            String downloadUrl,
            Map<Long, String> usernames
    ) {
        ExportRecordStatus status = ExportRecordStatus.fromCode(String.valueOf(entity.getStatus()));
        ExportSubmitRspDTO rspDTO = new ExportSubmitRspDTO();
        rspDTO.setRecordId(entity.getId());
        rspDTO.setExportBizCode(entity.getExportBizCode());
        rspDTO.setExportBizName(entity.getExportBizName());
        rspDTO.setFileName(entity.getFileName());
        rspDTO.setFileType(entity.getFileType());
        rspDTO.setStatus(entity.getStatus());
        rspDTO.setStatusName(status == null ? null : status.name());
        rspDTO.setContentType(entity.getContentType());
        rspDTO.setFileSize(entity.getFileSize());
        rspDTO.setDownloadCount(entity.getDownloadCount());
        rspDTO.setQuerySnapshotSummary(entity.getQuerySnapshotSummary());
        rspDTO.setFinishedTime(entity.getFinishedTime());
        rspDTO.setExpireTime(entity.getExpireTime());
        rspDTO.setCreateTime(entity.getCreateTime());
        rspDTO.setCreateBy(auditUsername(usernames, entity.getCreateBy()));
        rspDTO.setDownloadUrl(downloadUrl);
        return rspDTO;
    }

    private ExportTaskResult toTaskResult(ExportRecordEntity entity, String downloadUrl) {
        ExportRecordStatus status = ExportRecordStatus.fromCode(String.valueOf(entity.getStatus()));
        ExportTaskResult result = new ExportTaskResult();
        result.setRecordId(entity.getId());
        result.setExportBizCode(entity.getExportBizCode());
        result.setExportBizName(entity.getExportBizName());
        result.setFileName(entity.getFileName());
        result.setFileType(entity.getFileType());
        result.setStatus(entity.getStatus());
        result.setStatusName(status == null ? null : status.name());
        result.setContentType(entity.getContentType());
        result.setFileSize(entity.getFileSize());
        result.setDownloadCount(entity.getDownloadCount());
        result.setQuerySnapshotSummary(entity.getQuerySnapshotSummary());
        result.setDownloadUrl(downloadUrl);
        result.setFinishedTime(entity.getFinishedTime());
        result.setExpireTime(entity.getExpireTime());
        result.setCreateTime(entity.getCreateTime());
        result.setCreateBy(entity.getCreateBy());
        return result;
    }

    private DownloadLinkIssuedResult issueDownloadLink(ExportRecordEntity entity) {
        String downloadUrl = exportDownloadService.fetchDownloadUrl(entity, currentOperatorId());
        ExportRecordEntity refreshedRecord = exportRecordService.getActiveRequired(entity.getId());
        return new DownloadLinkIssuedResult(refreshedRecord, downloadUrl);
    }

    private Map<Long, String> auditUsernames(List<ExportRecordEntity> records) {
        return operatorUsernameResolver.resolveUsernames(records.stream()
                .map(ExportRecordEntity::getCreateBy)
                .toList());
    }

    private String auditUsername(Map<Long, String> usernames, Long operatorId) {
        return operatorId == null ? null : usernames.get(operatorId);
    }

    private record DownloadLinkIssuedResult(ExportRecordEntity record, String downloadUrl) {
    }
}
