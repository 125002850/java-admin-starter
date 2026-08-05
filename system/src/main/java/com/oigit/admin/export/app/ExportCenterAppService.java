package com.oigit.admin.export.app;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.exception.CommonErrorCode;
import com.oigit.admin.core.export.model.ExportTaskResult;
import com.oigit.admin.core.export.spi.ExportTaskSubmitter;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.support.DynamicQueryGuard;
import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.export.dto.req.ExportBatchDownloadReqDTO;
import com.oigit.admin.export.dto.req.ExportSubmitReqDTO;
import com.oigit.admin.export.dto.req.query.ExportRecordDynamicPageReqDTO;
import com.oigit.admin.export.dto.rsp.ExportBatchDownloadRspDTO;
import com.oigit.admin.export.dto.rsp.ExportDownloadRspDTO;
import com.oigit.admin.export.dto.rsp.ExportRecordRspDTO;
import com.oigit.admin.export.dto.rsp.ExportSubmitRspDTO;
import com.oigit.admin.export.app.query.ExportRecordSceneQueryMapper;
import com.oigit.admin.export.domain.model.ExportRecord;
import com.oigit.admin.export.domain.model.ExportRecordPage;
import com.oigit.admin.export.domain.repository.ExportRecordRepository;
import com.oigit.admin.export.enums.ExportCenterErrorCode;
import com.oigit.admin.export.enums.ExportDeleteReason;
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

    private final ExportExecutionAppService exportExecutionAppService;
    private final ExportDownloadAppService exportDownloadAppService;
    private final ExportBatchDownloadAppService exportBatchDownloadAppService;
    private final ExportRecordLifecycleAppService exportRecordLifecycleAppService;
    private final ExportRecordRepository exportRecordRepository;
    private final DynamicQueryGuard dynamicQueryGuard;
    private final ExportRecordSceneQueryMapper exportRecordSceneQueryMapper;

    public ExportCenterAppService(
            ExportExecutionAppService exportExecutionAppService,
            ExportDownloadAppService exportDownloadAppService,
            ExportBatchDownloadAppService exportBatchDownloadAppService,
            ExportRecordLifecycleAppService exportRecordLifecycleAppService,
            ExportRecordRepository exportRecordRepository,
            DynamicQueryGuard dynamicQueryGuard,
            ExportRecordSceneQueryMapper exportRecordSceneQueryMapper
    ) {
        this.exportExecutionAppService = exportExecutionAppService;
        this.exportDownloadAppService = exportDownloadAppService;
        this.exportBatchDownloadAppService = exportBatchDownloadAppService;
        this.exportRecordLifecycleAppService = exportRecordLifecycleAppService;
        this.exportRecordRepository = exportRecordRepository;
        this.dynamicQueryGuard = dynamicQueryGuard;
        this.exportRecordSceneQueryMapper = exportRecordSceneQueryMapper;
    }

    public ExportSubmitRspDTO submit(ExportSubmitReqDTO reqDTO) {
        ExportRecord record = exportExecutionAppService.submit(reqDTO.getSceneCode(), reqDTO.getQuery());
        return toSubmitRsp(record, null);
    }

    @Override
    public ExportTaskResult submit(String sceneCode, com.fasterxml.jackson.databind.JsonNode query) {
        ExportRecord record = exportExecutionAppService.submit(sceneCode, query);
        return toTaskResult(record, null);
    }

    @Transactional(readOnly = true)
    public PageResult<ExportRecordRspDTO> pageMyExports(ExportRecordDynamicPageReqDTO reqDTO) {
        QueryAst queryAst = exportRecordSceneQueryMapper.map(reqDTO, currentOperatorId());
        dynamicQueryGuard.validate(queryAst, exportRecordRepository.maxQueryComplexityScore());
        ExportRecordPage page = exportRecordRepository.page(queryAst);
        List<ExportRecordRspDTO> records = page.records().stream()
                .map(this::toRecordRsp)
                .toList();
        return new PageResult<>(records, page.total());
    }

    @Transactional(readOnly = true)
    public ExportRecordRspDTO detail(Long recordId) {
        ExportRecord record = exportRecordLifecycleAppService.getActiveRequired(recordId);
        record.requireOwnedBy(currentOperatorId());
        return toRecordRsp(record);
    }

    public ExportDownloadRspDTO download(Long recordId) {
        ExportRecord record = exportRecordLifecycleAppService.getActiveRequired(recordId);
        record.requireOwnedBy(currentOperatorId());
        String downloadUrl = exportDownloadAppService.fetchDownloadUrl(record, currentOperatorId());
        ExportDownloadRspDTO rspDTO = new ExportDownloadRspDTO();
        rspDTO.setRecordId(record.getId());
        rspDTO.setFileName(record.getFileName());
        rspDTO.setDownloadUrl(downloadUrl);
        return rspDTO;
    }

    public ExportBatchDownloadRspDTO batchDownload(ExportBatchDownloadReqDTO reqDTO) {
        ExportRecord record = exportBatchDownloadAppService.submitPackageRecords(reqDTO.getIds(), currentOperatorId());
        ExportBatchDownloadRspDTO rspDTO = new ExportBatchDownloadRspDTO();
        rspDTO.setRecordId(record.getId());
        rspDTO.setStatus(record.getStatus());
        rspDTO.setFileName(record.getFileName());
        rspDTO.setContentType(record.getContentType());
        rspDTO.setFileSize(record.getFileSize());
        return rspDTO;
    }

    @Transactional
    public void delete(List<Long> recordIds) {
        List<Long> normalizedRecordIds = normalizeRecordIds(recordIds);
        List<ExportRecord> records = listRequiredRecords(normalizedRecordIds);
        for (ExportRecord record : records) {
            record.requireOwnedBy(currentOperatorId());
        }
        exportRecordLifecycleAppService.markDeleted(normalizedRecordIds, ExportDeleteReason.MANUAL);
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

    private List<ExportRecord> listRequiredRecords(List<Long> recordIds) {
        List<ExportRecord> records = exportRecordLifecycleAppService.listActiveByIds(recordIds);
        if (records.size() != recordIds.size()) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND);
        }
        Map<Long, ExportRecord> recordMap = new LinkedHashMap<>();
        for (ExportRecord record : records) {
            recordMap.put(record.getId(), record);
        }
        List<ExportRecord> orderedRecords = new ArrayList<>();
        for (Long recordId : recordIds) {
            ExportRecord record = recordMap.get(recordId);
            if (record == null) {
                throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND);
            }
            orderedRecords.add(record);
        }
        return orderedRecords;
    }

    private Long currentOperatorId() {
        Long operatorId = OperatorContext.getOperatorId();
        return operatorId == null ? FALLBACK_OPERATOR_ID : operatorId;
    }

    private ExportRecordRspDTO toRecordRsp(ExportRecord entity) {
        ExportRecordRspDTO rspDTO = new ExportRecordRspDTO();
        rspDTO.setRecordId(entity.getId());
        rspDTO.setExportBizCode(entity.getExportBizCode());
        rspDTO.setExportBizName(entity.getExportBizName());
        rspDTO.setFileName(entity.getFileName());
        rspDTO.setFileType(entity.getFileType());
        rspDTO.setStatus(entity.getStatus());
        rspDTO.setContentType(entity.getContentType());
        rspDTO.setFileSize(entity.getFileSize());
        rspDTO.setDownloadCount(entity.getDownloadCount());
        rspDTO.setQuerySnapshotSummary(entity.getQuerySnapshotSummary());
        rspDTO.setFinishedTime(entity.getFinishedTime());
        rspDTO.setExpireTime(entity.getExpireTime());
        rspDTO.setCreateTime(entity.getCreateTime());
        rspDTO.setUpdateTime(entity.getUpdateTime());
        rspDTO.setCreateById(entity.getCreateBy());
        rspDTO.setUpdateById(entity.getUpdateBy());
        return rspDTO;
    }

    private ExportSubmitRspDTO toSubmitRsp(ExportRecord entity, String downloadUrl) {
        ExportSubmitRspDTO rspDTO = new ExportSubmitRspDTO();
        rspDTO.setRecordId(entity.getId());
        rspDTO.setExportBizCode(entity.getExportBizCode());
        rspDTO.setExportBizName(entity.getExportBizName());
        rspDTO.setFileName(entity.getFileName());
        rspDTO.setFileType(entity.getFileType());
        rspDTO.setStatus(entity.getStatus());
        rspDTO.setContentType(entity.getContentType());
        rspDTO.setFileSize(entity.getFileSize());
        rspDTO.setDownloadCount(entity.getDownloadCount());
        rspDTO.setQuerySnapshotSummary(entity.getQuerySnapshotSummary());
        rspDTO.setFinishedTime(entity.getFinishedTime());
        rspDTO.setExpireTime(entity.getExpireTime());
        rspDTO.setCreateTime(entity.getCreateTime());
        rspDTO.setUpdateTime(entity.getUpdateTime());
        rspDTO.setCreateById(entity.getCreateBy());
        rspDTO.setUpdateById(entity.getUpdateBy());
        rspDTO.setDownloadUrl(downloadUrl);
        return rspDTO;
    }

    private ExportTaskResult toTaskResult(ExportRecord entity, String downloadUrl) {
        ExportTaskResult result = new ExportTaskResult();
        result.setRecordId(entity.getId());
        result.setExportBizCode(entity.getExportBizCode());
        result.setExportBizName(entity.getExportBizName());
        result.setFileName(entity.getFileName());
        result.setFileType(entity.getFileType());
        result.setStatus(entity.getStatus() == null ? null : entity.getStatus().getIntCode());
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

}
