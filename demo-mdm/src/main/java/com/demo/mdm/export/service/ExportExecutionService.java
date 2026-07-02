package com.demo.mdm.export.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.demo.core.exception.BizException;
import com.demo.core.exception.CommonErrorCode;
import com.demo.core.export.dto.ExportOptionsReqDTO;
import com.demo.core.export.dto.ExportRangeReqDTO;
import com.demo.core.export.model.ExportColumn;
import com.demo.core.export.model.ExportMeta;
import com.demo.core.export.model.ExportRenderRequest;
import com.demo.core.export.model.ExportScope;
import com.demo.core.export.model.ExportStoreRequest;
import com.demo.core.export.model.ExportStoredFile;
import com.demo.core.export.model.RenderedExportFile;
import com.demo.core.export.spi.ExportFileSink;
import com.demo.core.export.spi.ExportHandler;
import com.demo.core.export.spi.ExportRenderer;
import com.demo.core.export.spi.ExportRendererRegistry;
import com.demo.core.export.spi.ExportSceneRegistry;
import com.demo.core.export.spi.PackageableExportHandler;
import com.demo.core.query.ast.QueryAst;
import com.demo.core.query.support.DynamicQuerySummaryRenderer;
import com.demo.mdm.export.enums.ExportCenterErrorCode;
import com.demo.mdm.export.infra.entity.ExportRecordEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportExecutionService {

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

    public ExportExecutionService(
            ExportSceneRegistry exportSceneRegistry,
            ExportRendererRegistry exportRendererRegistry,
            ExportFileSink exportFileSink,
            ExportRecordService exportRecordService,
            ObjectMapper objectMapper,
            DynamicQuerySummaryRenderer dynamicQuerySummaryRenderer
    ) {
        this.exportSceneRegistry = exportSceneRegistry;
        this.exportRendererRegistry = exportRendererRegistry;
        this.exportFileSink = exportFileSink;
        this.exportRecordService = exportRecordService;
        this.objectMapper = objectMapper;
        this.dynamicQuerySummaryRenderer = dynamicQuerySummaryRenderer;
    }

    public ExportRecordEntity execute(String sceneCode, JsonNode queryNode) {
        ExportHandler<?> handler = resolveHandler(sceneCode);
        return executeInternal(handler, queryNode);
    }

    private <Q> ExportRecordEntity executeInternal(ExportHandler<Q> handler, JsonNode queryNode) {
        Q query = convertQuery(handler, queryNode);
        handler.validate(query);

        ExportMeta meta = handler.buildMeta(query);
        validateMeta(meta);
        if (isPackageMode(query)) {
            return executePackageInternal(handler, query, meta);
        }

        List<ExportColumn> columns = defaultIfNull(handler.columns(query));
        List<?> rows = defaultIfNull(handler.queryRows(query));
        String querySnapshotJson = serializeQuery(query);

        ExportRecordEntity record = buildProcessingRecord(handler, meta, querySnapshotJson, query);
        Long recordId = exportRecordService.createProcessingRecord(record);
        try {
            ExportRenderer renderer = resolveRenderer(record.getFileType());
            ExportRenderRequest renderRequest = new ExportRenderRequest();
            renderRequest.setFileName(record.getFileName());
            renderRequest.setColumns(columns);
            renderRequest.setRows(rows);
            RenderedExportFile renderedExportFile = renderer.render(renderRequest);

            ExportStoreRequest storeRequest = new ExportStoreRequest();
            storeRequest.setBizPath(resolveBizPath(handler.sceneCode()));
            ExportStoredFile storedFile = exportFileSink.store(renderedExportFile, storeRequest);

            exportRecordService.markSuccess(
                recordId,
                storedFile.getObjectKey(),
                storedFile.getContentType(),
                storedFile.getFileSize(),
                storedFile.getStorageType()
            );
            return exportRecordService.getRequired(recordId);
        } catch (BizException ex) {
            markFailedQuietly(recordId, ex.getErrorCode().getMsg(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            markFailedQuietly(recordId, "EXPORT_EXECUTION_FAILED", ex.getMessage());
            throw new BizException(ExportCenterErrorCode.EXPORT_EXECUTION_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    private <Q> ExportRecordEntity executePackageInternal(ExportHandler<Q> handler, Q query, ExportMeta meta) {
        if (!(query instanceof ExportOptionsReqDTO options)
                || !(handler instanceof PackageableExportHandler<?> packageableHandler)) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }
        validatePackageMeta(meta);

        ExportMeta packageMeta = toPackageMeta(meta);
        String querySnapshotJson = serializeQuery(query);
        ExportRecordEntity record = buildProcessingRecord(handler, packageMeta, querySnapshotJson, query);
        Long recordId = exportRecordService.createProcessingRecord(record);
        try {
            List<ExportColumn> columns = defaultIfNull(handler.columns(query));
            RenderedExportFile renderedExportFile = renderPackageFile(
                    (PackageableExportHandler<Q>) packageableHandler,
                    query,
                    packageMeta,
                    columns,
                    options
            );

            ExportStoreRequest storeRequest = new ExportStoreRequest();
            storeRequest.setBizPath(resolveBizPath(handler.sceneCode()));
            ExportStoredFile storedFile = exportFileSink.store(renderedExportFile, storeRequest);

            exportRecordService.markSuccess(
                recordId,
                storedFile.getObjectKey(),
                storedFile.getContentType(),
                storedFile.getFileSize(),
                storedFile.getStorageType()
            );
            return exportRecordService.getRequired(recordId);
        } catch (BizException ex) {
            markFailedQuietly(recordId, ex.getErrorCode().getMsg(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            markFailedQuietly(recordId, "EXPORT_EXECUTION_FAILED", ex.getMessage());
            throw new BizException(ExportCenterErrorCode.EXPORT_EXECUTION_FAILED);
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

        byte[] content = zipCsvChunks(handler, query, csvRenderer, columns, packageMeta.getFileName(), chunkSize, window);
        RenderedExportFile file = new RenderedExportFile();
        file.setFileName(packageMeta.getFileName());
        file.setFileType(ZIP_FILE_TYPE);
        file.setContentType(ZIP_CONTENT_TYPE);
        file.setContent(content);
        file.setFileSize(content.length);
        return file;
    }

    private <Q> byte[] zipCsvChunks(
            PackageableExportHandler<Q> handler,
            Q query,
            ExportRenderer csvRenderer,
            List<ExportColumn> columns,
            String zipFileName,
            int chunkSize,
            RangeWindow window
    ) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            if (window.totalRows() == 0) {
                writeCsvEntry(zipOutputStream, csvRenderer, columns, List.of(), buildChunkFileName(zipFileName, 0, 0));
                return outputStream.toByteArray();
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
        return outputStream.toByteArray();
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
        RenderedExportFile renderedCsv = csvRenderer.render(renderRequest);
        ZipEntry zipEntry = new ZipEntry(sanitizeEntryName(renderedCsv.getFileName()));
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(renderedCsv.getContent());
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
        } catch (Exception ignored) {
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
