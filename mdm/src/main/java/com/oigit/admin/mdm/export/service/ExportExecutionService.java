package com.oigit.admin.mdm.export.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.exception.CommonErrorCode;
import com.oigit.admin.core.export.dto.ExportOptionsReqDTO;
import com.oigit.admin.core.export.dto.ExportRangeReqDTO;
import com.oigit.admin.core.export.model.ExportColumn;
import com.oigit.admin.core.export.model.ExportMeta;
import com.oigit.admin.core.export.model.ExportRenderRequest;
import com.oigit.admin.core.export.model.ExportScope;
import com.oigit.admin.core.export.model.ExportStoreRequest;
import com.oigit.admin.core.export.model.ExportStoredFile;
import com.oigit.admin.core.export.model.RenderedExportFile;
import com.oigit.admin.core.export.spi.ExportFileSink;
import com.oigit.admin.core.export.spi.ExportHandler;
import com.oigit.admin.core.export.spi.ExportRenderer;
import com.oigit.admin.core.export.spi.ExportRendererRegistry;
import com.oigit.admin.core.export.spi.ExportSceneRegistry;
import com.oigit.admin.core.export.spi.PackageableExportHandler;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.support.DynamicQuerySummaryRenderer;
import com.oigit.admin.mdm.export.enums.ExportCenterErrorCode;
import com.oigit.admin.mdm.export.infra.entity.ExportRecordEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExportExecutionService.class);
    private static final int DEFAULT_EXPIRE_SECONDS = 3600;
    private static final int DEFAULT_PACKAGE_CHUNK_SIZE = 5000;
    private static final int MAX_PACKAGE_CHUNK_SIZE = 5000;
    private static final int MAX_QUERY_SUMMARY_LENGTH = 512;
    private static final int MAX_FAIL_MESSAGE_LENGTH = 255;
    private static final String CSV_FILE_TYPE = "csv";
    private static final String ZIP_FILE_TYPE = "zip";
    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final Pattern BIZ_PATH_UNSAFE_PATTERN = Pattern.compile("[^A-Za-z0-9_-]");

    private final ExportSceneRegistry exportSceneRegistry;
    private final ExportRendererRegistry exportRendererRegistry;
    private final ExportFileSink exportFileSink;
    private final ExportRecordService exportRecordService;
    private final ObjectMapper objectMapper;
    private final DynamicQuerySummaryRenderer dynamicQuerySummaryRenderer;
    private final ExportTaskDispatcher exportTaskDispatcher;

    public ExportExecutionService(
            ExportSceneRegistry exportSceneRegistry,
            ExportRendererRegistry exportRendererRegistry,
            ExportFileSink exportFileSink,
            ExportRecordService exportRecordService,
            ObjectMapper objectMapper,
            DynamicQuerySummaryRenderer dynamicQuerySummaryRenderer,
            ExportTaskDispatcher exportTaskDispatcher
    ) {
        this.exportSceneRegistry = exportSceneRegistry;
        this.exportRendererRegistry = exportRendererRegistry;
        this.exportFileSink = exportFileSink;
        this.exportRecordService = exportRecordService;
        this.objectMapper = objectMapper;
        this.dynamicQuerySummaryRenderer = dynamicQuerySummaryRenderer;
        this.exportTaskDispatcher = exportTaskDispatcher;
    }

    public ExportRecordEntity submit(String sceneCode, JsonNode queryNode) {
        ExportHandler<?> handler = resolveHandler(sceneCode);
        return submitInternal(handler, queryNode);
    }

    @SuppressWarnings("unchecked")
    private <Q> ExportRecordEntity submitInternal(ExportHandler<Q> handler, JsonNode queryNode) {
        Q query = convertQuery(handler, queryNode);
        handler.validate(query);

        ExportMeta meta = handler.buildMeta(query);
        validateMeta(meta);
        if (isPackageMode(query)) {
            if (!(query instanceof ExportOptionsReqDTO options)
                    || !(handler instanceof PackageableExportHandler<?> packageableHandler)) {
                throw new BizException(CommonErrorCode.PARAM_ERROR);
            }
            validatePackageMeta(meta);
            ExportMeta packageMeta = toPackageMeta(meta);
            ExportRecordEntity record = createProcessingRecord(handler, query, packageMeta);
            dispatchTask(
                    record.getId(),
                    () -> executePackageTask(
                            (PackageableExportHandler<Q>) packageableHandler,
                            query,
                            packageMeta,
                            options,
                            record.getId()
                    )
            );
            return record;
        }

        ExportRecordEntity record = createProcessingRecord(handler, query, meta);
        dispatchTask(record.getId(), () -> executeStandardTask(handler, query, record, record.getId()));
        return record;
    }

    private <Q> ExportRecordEntity createProcessingRecord(ExportHandler<Q> handler, Q query, ExportMeta meta) {
        String querySnapshotJson = serializeQuery(query);
        ExportRecordEntity record = buildProcessingRecord(handler, meta, querySnapshotJson, query);
        exportRecordService.createProcessingRecord(record);
        return record;
    }

    private void dispatchTask(Long recordId, Runnable task) {
        try {
            exportTaskDispatcher.dispatch(task);
        } catch (RuntimeException ex) {
            markFailedQuietly(recordId, "EXPORT_TASK_REJECTED", ex.getMessage());
            throw new BizException(ExportCenterErrorCode.EXPORT_EXECUTION_FAILED);
        }
    }

    private <Q> void executeStandardTask(
            ExportHandler<Q> handler,
            Q query,
            ExportRecordEntity record,
            Long recordId
    ) {
        ExportStoredFile storedFile = null;
        try {
            List<ExportColumn> columns = defaultIfNull(handler.columns(query));
            List<?> rows = defaultIfNull(handler.queryRows(query));
            ExportRenderer renderer = resolveRenderer(record.getFileType());
            ExportRenderRequest renderRequest = new ExportRenderRequest();
            renderRequest.setFileName(record.getFileName());
            renderRequest.setColumns(columns);
            renderRequest.setRows(rows);

            ExportStoreRequest storeRequest = new ExportStoreRequest();
            storeRequest.setBizPath(resolveBizPath(handler.sceneCode()));
            try (RenderedExportFile renderedExportFile = renderer.render(renderRequest)) {
                storedFile = exportFileSink.store(renderedExportFile, storeRequest);
            }

            exportRecordService.markSuccess(
                    recordId,
                    storedFile.getObjectKey(),
                    storedFile.getContentType(),
                    storedFile.getFileSize(),
                    storedFile.getStorageType()
            );
        } catch (BizException ex) {
            handleTaskFailure(recordId, storedFile, String.valueOf(ex.getErrorCode().getCode()), ex.getMessage(), ex);
        } catch (Exception ex) {
            handleTaskFailure(recordId, storedFile, "EXPORT_EXECUTION_FAILED", ex.getMessage(), ex);
        }
    }

    private <Q> void executePackageTask(
            PackageableExportHandler<Q> handler,
            Q query,
            ExportMeta packageMeta,
            ExportOptionsReqDTO options,
            Long recordId
    ) {
        ExportStoredFile storedFile = null;
        try {
            List<ExportColumn> columns = defaultIfNull(handler.columns(query));
            ExportStoreRequest storeRequest = new ExportStoreRequest();
            storeRequest.setBizPath(resolveBizPath(handler.sceneCode()));
            try (RenderedExportFile renderedExportFile = renderPackageFile(handler, query, packageMeta, columns, options)) {
                storedFile = exportFileSink.store(renderedExportFile, storeRequest);
            }

            exportRecordService.markSuccess(
                    recordId,
                    storedFile.getObjectKey(),
                    storedFile.getContentType(),
                    storedFile.getFileSize(),
                    storedFile.getStorageType()
            );
        } catch (BizException ex) {
            handleTaskFailure(recordId, storedFile, String.valueOf(ex.getErrorCode().getCode()), ex.getMessage(), ex);
        } catch (Exception ex) {
            handleTaskFailure(recordId, storedFile, "EXPORT_EXECUTION_FAILED", ex.getMessage(), ex);
        }
    }

    private ExportHandler<?> resolveHandler(String sceneCode) {
        try {
            return exportSceneRegistry.getRequired(sceneCode);
        } catch (NoSuchElementException ex) {
            throw new BizException(ExportCenterErrorCode.EXPORT_SCENE_NOT_FOUND);
        }
    }

    private ExportRenderer resolveRenderer(String fileType) {
        try {
            return exportRendererRegistry.getRequired(fileType);
        } catch (NoSuchElementException ex) {
            throw new BizException(ExportCenterErrorCode.EXPORT_FILE_TYPE_NOT_SUPPORTED);
        }
    }

    private <Q> Q convertQuery(ExportHandler<Q> handler, JsonNode queryNode) {
        try {
            JsonNode source = queryNode == null || queryNode.isNull() ? objectMapper.createObjectNode() : queryNode;
            return objectMapper.treeToValue(source, handler.queryType());
        } catch (JsonProcessingException ex) {
            throw new BizException(ExportCenterErrorCode.EXPORT_QUERY_INVALID);
        }
    }

    private <Q> ExportRecordEntity buildProcessingRecord(
            ExportHandler<Q> handler,
            ExportMeta meta,
            String querySnapshotJson,
            Q query
    ) {
        ExportRecordEntity entity = new ExportRecordEntity();
        entity.setExportBizCode(handler.sceneCode());
        entity.setExportBizName(handler.sceneName());
        entity.setFileType(normalizeFileType(meta.getFileType()));
        entity.setFileName(ensureFileName(meta.getFileName(), entity.getFileType()));
        entity.setExpireSeconds(resolveExpireSeconds(meta.getExpireSeconds()));
        entity.setExpireTime(LocalDateTime.now().plusSeconds(entity.getExpireSeconds()));
        entity.setQuerySnapshotJson(querySnapshotJson);
        entity.setQuerySnapshotSummary(resolveSummary(handler, query, meta.getQuerySnapshotSummary(), querySnapshotJson));
        entity.setDownloadCount(0);
        entity.setDeleted(0L);
        return entity;
    }

    private void validateMeta(ExportMeta meta) {
        if (meta == null || !StringUtils.hasText(meta.getFileName()) || !StringUtils.hasText(meta.getFileType())) {
            throw new BizException(ExportCenterErrorCode.EXPORT_EXECUTION_FAILED);
        }
    }

    private void validatePackageMeta(ExportMeta meta) {
        if (!CSV_FILE_TYPE.equalsIgnoreCase(meta.getFileType())) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }
    }

    private ExportMeta toPackageMeta(ExportMeta meta) {
        ExportMeta packageMeta = new ExportMeta();
        packageMeta.setFileType(ZIP_FILE_TYPE);
        packageMeta.setFileName(replaceExtension(meta.getFileName(), ZIP_FILE_TYPE));
        packageMeta.setExpireSeconds(meta.getExpireSeconds());
        packageMeta.setQuerySnapshotSummary(meta.getQuerySnapshotSummary());
        return packageMeta;
    }

    private String ensureFileName(String fileName, String fileType) {
        String normalizedFileName = fileName.trim();
        String suffix = "." + fileType;
        return normalizedFileName.toLowerCase().endsWith(suffix) ? normalizedFileName : normalizedFileName + suffix;
    }

    private String normalizeFileType(String fileType) {
        return fileType.trim().toLowerCase();
    }

    private boolean isPackageMode(Object query) {
        return query instanceof ExportOptionsReqDTO options && Boolean.TRUE.equals(options.getPackageMode());
    }

    private <Q> RenderedExportFile renderPackageFile(
            PackageableExportHandler<Q> handler,
            Q query,
            ExportMeta packageMeta,
            List<ExportColumn> columns,
            ExportOptionsReqDTO options
    ) throws IOException {
        ExportRenderer csvRenderer = resolveRenderer(CSV_FILE_TYPE);
        int chunkSize = resolveChunkSize(options.getChunkSize());
        long totalRows = handler.countRows(query);
        RangeWindow window = resolveRangeWindow(options.getRange(), totalRows);

        Path contentPath = Files.createTempFile("oig-export-package-", ".zip");
        boolean completed = false;
        try {
            zipCsvChunks(handler, query, csvRenderer, columns, packageMeta.getFileName(), chunkSize, window, contentPath);
            RenderedExportFile file = new RenderedExportFile();
            file.setFileName(packageMeta.getFileName());
            file.setFileType(ZIP_FILE_TYPE);
            file.setContentType(ZIP_CONTENT_TYPE);
            file.setContentPath(contentPath);
            file.setFileSize(Files.size(contentPath));
            completed = true;
            return file;
        } finally {
            if (!completed) {
                Files.deleteIfExists(contentPath);
            }
        }
    }

    private <Q> void zipCsvChunks(
            PackageableExportHandler<Q> handler,
            Q query,
            ExportRenderer csvRenderer,
            List<ExportColumn> columns,
            String zipFileName,
            int chunkSize,
            RangeWindow window,
            Path contentPath
    ) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(contentPath), StandardCharsets.UTF_8)) {
            if (window.totalRows() == 0) {
                writeCsvEntry(zipOutputStream, csvRenderer, columns, List.of(), buildChunkFileName(zipFileName, 0, 0));
                return;
            }
            long chunkStart = window.startRow();
            while (chunkStart <= window.endRow()) {
                long chunkEnd = Math.min(window.endRow(), chunkStart + chunkSize - 1L);
                Q chunkQuery = copyWithRange(handler, query, chunkStart, chunkEnd);
                List<?> rows = defaultIfNull(handler.queryRows(chunkQuery));
                writeCsvEntry(
                        zipOutputStream,
                        csvRenderer,
                        columns,
                        rows,
                        buildChunkFileName(zipFileName, chunkStart, chunkEnd)
                );
                chunkStart = chunkEnd + 1L;
            }
        }
    }

    private void writeCsvEntry(
            ZipOutputStream zipOutputStream,
            ExportRenderer csvRenderer,
            List<ExportColumn> columns,
            List<?> rows,
            String fileName
    ) throws IOException {
        ExportRenderRequest renderRequest = new ExportRenderRequest();
        renderRequest.setFileName(fileName);
        renderRequest.setColumns(columns);
        renderRequest.setRows(rows);
        ZipEntry zipEntry = new ZipEntry(sanitizeEntryName(fileName));
        zipOutputStream.putNextEntry(zipEntry);
        csvRenderer.render(renderRequest, zipOutputStream);
        zipOutputStream.closeEntry();
    }

    private int resolveChunkSize(Integer chunkSize) {
        if (chunkSize == null) {
            return DEFAULT_PACKAGE_CHUNK_SIZE;
        }
        if (chunkSize <= 0 || chunkSize > MAX_PACKAGE_CHUNK_SIZE) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }
        return chunkSize;
    }

    private RangeWindow resolveRangeWindow(ExportRangeReqDTO range, long totalRows) {
        if (range == null) {
            return new RangeWindow(totalRows == 0 ? 0 : 1, totalRows, totalRows);
        }
        Long startRow = range.getStartRow();
        Long endRow = range.getEndRow();
        if (startRow == null || endRow == null || startRow <= 0 || endRow <= 0 || startRow > endRow) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }
        if (totalRows == 0 || startRow > totalRows) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }
        return new RangeWindow(startRow, Math.min(endRow, totalRows), totalRows);
    }

    private <Q> Q copyWithRange(PackageableExportHandler<Q> handler, Q query, long startRow, long endRow) {
        try {
            Q copiedQuery = objectMapper.treeToValue(objectMapper.valueToTree(query), handler.queryType());
            if (!(copiedQuery instanceof ExportOptionsReqDTO options)) {
                throw new BizException(CommonErrorCode.PARAM_ERROR);
            }
            ExportRangeReqDTO range = new ExportRangeReqDTO();
            range.setStartRow(startRow);
            range.setEndRow(endRow);
            options.setRange(range);
            options.setPackageMode(false);
            options.setChunkSize(null);
            return copiedQuery;
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new BizException(ExportCenterErrorCode.EXPORT_QUERY_INVALID);
        }
    }

    private String buildChunkFileName(String zipFileName, long startRow, long endRow) {
        return removeExtension(zipFileName) + "-" + startRow + "-" + endRow + "." + CSV_FILE_TYPE;
    }

    private String replaceExtension(String fileName, String newExtension) {
        return removeExtension(fileName) + "." + newExtension;
    }

    private String removeExtension(String fileName) {
        String normalized = fileName == null ? "导出文件" : fileName.trim();
        int extensionIndex = normalized.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return normalized;
        }
        return normalized.substring(0, extensionIndex);
    }

    private String sanitizeEntryName(String fileName) {
        String normalized = StringUtils.hasText(fileName) ? fileName.trim() : "导出文件";
        normalized = normalized
                .replace('\\', '-')
                .replace('/', '-')
                .replaceAll("[\\r\\n\\t\\x00-\\x1F]+", "-");
        return StringUtils.hasText(normalized) ? normalized : "导出文件";
    }

    private Integer resolveExpireSeconds(Integer expireSeconds) {
        if (expireSeconds == null || expireSeconds <= 0) {
            return DEFAULT_EXPIRE_SECONDS;
        }
        return expireSeconds;
    }

    private <Q> String resolveSummary(
            ExportHandler<Q> handler,
            Q query,
            String querySnapshotSummary,
            String querySnapshotJson
    ) {
        String summary = StringUtils.hasText(querySnapshotSummary) ? querySnapshotSummary : querySnapshotJson;
        if (ExportScope.DYNAMIC_QUERY_SUMMARY.equals(summary)) {
            QueryAst queryAst = handler.summaryQueryAst(query);
            String renderedSummary = dynamicQuerySummaryRenderer.render(queryAst, handler.summarySceneQueryDefinition());
            if (StringUtils.hasText(renderedSummary)) {
                summary = renderedSummary;
            }
        }
        return truncate(summary, MAX_QUERY_SUMMARY_LENGTH);
    }

    private String serializeQuery(Object query) {
        try {
            return objectMapper.writeValueAsString(query);
        } catch (JsonProcessingException ex) {
            throw new BizException(ExportCenterErrorCode.EXPORT_QUERY_INVALID);
        }
    }

    private String resolveBizPath(String sceneCode) {
        return "export/" + BIZ_PATH_UNSAFE_PATTERN.matcher(sceneCode).replaceAll("_");
    }

    private void markFailedQuietly(Long recordId, String failCode, String failMessage) {
        try {
            exportRecordService.markFailed(recordId, truncate(failCode, 64), truncate(failMessage, MAX_FAIL_MESSAGE_LENGTH));
        } catch (Exception ex) {
            log.error("failed to persist export task failure, recordId={}", recordId, ex);
        }
    }

    private void handleTaskFailure(
            Long recordId,
            ExportStoredFile storedFile,
            String failCode,
            String failMessage,
            Exception cause
    ) {
        cleanupStoredFileQuietly(storedFile);
        markFailedQuietly(recordId, failCode, failMessage);
        log.warn("export task failed, recordId={}", recordId, cause);
    }

    private void cleanupStoredFileQuietly(ExportStoredFile storedFile) {
        if (storedFile == null || !StringUtils.hasText(storedFile.getObjectKey())) {
            return;
        }
        try {
            exportFileSink.delete(storedFile.getObjectKey());
        } catch (Exception ex) {
            log.warn("failed to clean uploaded export object, objectKey={}", storedFile.getObjectKey(), ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private <T> List<T> defaultIfNull(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }

    private record RangeWindow(long startRow, long endRow, long totalRows) {
    }
}
