package com.oigit.admin.export.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.export.model.ExportStoreRequest;
import com.oigit.admin.core.export.model.ExportStoredFile;
import com.oigit.admin.core.export.model.RenderedExportFile;
import com.oigit.admin.core.export.spi.ExportFileAccessor;
import com.oigit.admin.core.export.spi.ExportFileSink;
import com.oigit.admin.export.domain.gateway.ExportTaskDispatcher;
import com.oigit.admin.export.domain.model.ExportRecord;
import com.oigit.admin.export.enums.ExportCenterErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportBatchDownloadAppService {

    private static final Logger log = LoggerFactory.getLogger(ExportBatchDownloadAppService.class);
    private static final int MAX_BATCH_RECORD_COUNT = 50;
    private static final long MAX_BATCH_TOTAL_BYTES = 200L * 1024 * 1024;
    private static final int COPY_BUFFER_SIZE = 64 * 1024;
    private static final int DEFAULT_EXPIRE_SECONDS = 3600;
    private static final int MAX_FAIL_MESSAGE_LENGTH = 255;
    private static final String BATCH_EXPORT_BIZ_CODE = "mdm.export-center.batch-download";
    private static final String BATCH_EXPORT_BIZ_NAME = "导出中心批量下载";
    private static final String BATCH_FILE_TYPE = "zip";
    private static final String BATCH_CONTENT_TYPE = "application/zip";
    private static final String BATCH_BIZ_PATH = "export/mdm_export_center_batch_download";
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExportRecordLifecycleAppService exportRecordLifecycleAppService;
    private final ExportFileAccessor exportFileAccessor;
    private final ExportFileSink exportFileSink;
    private final ExportTaskDispatcher exportTaskDispatcher;
    private final ObjectMapper objectMapper;

    public ExportBatchDownloadAppService(
            ExportRecordLifecycleAppService exportRecordLifecycleAppService,
            ExportFileAccessor exportFileAccessor,
            ExportFileSink exportFileSink,
            ExportTaskDispatcher exportTaskDispatcher,
            ObjectMapper objectMapper
    ) {
        this.exportRecordLifecycleAppService = exportRecordLifecycleAppService;
        this.exportFileAccessor = exportFileAccessor;
        this.exportFileSink = exportFileSink;
        this.exportTaskDispatcher = exportTaskDispatcher;
        this.objectMapper = objectMapper;
    }

    public ExportRecord submitPackageRecords(List<Long> recordIds, Long operatorId) {
        List<Long> normalizedRecordIds = normalizeRecordIds(recordIds);
        List<ExportRecord> records = listOwnedDownloadableRecords(normalizedRecordIds, operatorId);
        assertTotalFileSizeWithinLimit(records);

        ExportRecord taskRecord = buildProcessingRecord(normalizedRecordIds);
        exportRecordLifecycleAppService.createProcessingRecord(taskRecord);
        List<BatchSourceFile> sourceFiles = records.stream()
                .map(record -> new BatchSourceFile(record.getId(), record.getFileName(), record.getObjectKey()))
                .toList();
        try {
            exportTaskDispatcher.dispatch(() -> executePackageTask(
                    taskRecord.getId(),
                    taskRecord.getFileName(),
                    sourceFiles,
                    operatorId
            ));
        } catch (RuntimeException ex) {
            markFailedQuietly(taskRecord.getId(), "EXPORT_TASK_REJECTED", ex.getMessage());
            throw new BizException(ExportCenterErrorCode.EXPORT_EXECUTION_FAILED);
        }
        return taskRecord;
    }

    private void executePackageTask(
            Long taskRecordId,
            String taskFileName,
            List<BatchSourceFile> sourceFiles,
            Long operatorId
    ) {
        ExportStoredFile storedFile = null;
        try {
            ExportStoreRequest storeRequest = new ExportStoreRequest();
            storeRequest.setBizPath(BATCH_BIZ_PATH);
            try (RenderedExportFile zipFile = renderZipFile(taskFileName, sourceFiles)) {
                storedFile = exportFileSink.store(zipFile, storeRequest);
            }
            exportRecordLifecycleAppService.markBatchSuccess(
                    taskRecordId,
                    storedFile.getObjectKey(),
                    storedFile.getContentType(),
                    storedFile.getFileSize(),
                    storedFile.getStorageType(),
                    sourceFiles.stream().map(BatchSourceFile::recordId).toList(),
                    operatorId
            );
        } catch (BizException ex) {
            handleTaskFailure(
                    taskRecordId,
                    storedFile,
                    String.valueOf(ex.getErrorCode().getCode()),
                    ex.getMessage(),
                    ex
            );
        } catch (Exception ex) {
            handleTaskFailure(taskRecordId, storedFile, "EXPORT_EXECUTION_FAILED", ex.getMessage(), ex);
        }
    }

    private ExportRecord buildProcessingRecord(List<Long> sourceRecordIds) {
        ExportRecord record = new ExportRecord();
        record.setExportBizCode(BATCH_EXPORT_BIZ_CODE);
        record.setExportBizName(BATCH_EXPORT_BIZ_NAME);
        record.setFileName(buildBatchFileName());
        record.setFileType(BATCH_FILE_TYPE);
        record.setQuerySnapshotJson(serializeSourceRecordIds(sourceRecordIds));
        record.setQuerySnapshotSummary("批量下载 " + sourceRecordIds.size() + " 个导出文件");
        record.setDownloadCount(0);
        record.setExpireSeconds(DEFAULT_EXPIRE_SECONDS);
        record.setExpireTime(LocalDateTime.now().plusSeconds(DEFAULT_EXPIRE_SECONDS));
        record.setDeleted(0L);
        return record;
    }

    private String serializeSourceRecordIds(List<Long> sourceRecordIds) {
        try {
            return objectMapper.writeValueAsString(Map.of("recordIds", sourceRecordIds));
        } catch (JsonProcessingException ex) {
            throw new BizException(ExportCenterErrorCode.EXPORT_QUERY_INVALID);
        }
    }

    private List<Long> normalizeRecordIds(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            throw new BizException(ExportCenterErrorCode.EXPORT_BATCH_RECORD_EMPTY);
        }
        Set<Long> distinctIds = new LinkedHashSet<>();
        for (Long recordId : recordIds) {
            if (recordId == null) {
                throw new BizException(ExportCenterErrorCode.EXPORT_BATCH_RECORD_EMPTY);
            }
            distinctIds.add(recordId);
        }
        if (distinctIds.isEmpty()) {
            throw new BizException(ExportCenterErrorCode.EXPORT_BATCH_RECORD_EMPTY);
        }
        if (distinctIds.size() > MAX_BATCH_RECORD_COUNT) {
            throw new BizException(ExportCenterErrorCode.EXPORT_BATCH_RECORD_LIMIT_EXCEEDED);
        }
        return new ArrayList<>(distinctIds);
    }

    private List<ExportRecord> listOwnedDownloadableRecords(List<Long> recordIds, Long operatorId) {
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
            ensureOwnedBy(record, operatorId);
            ensureDownloadable(record);
            orderedRecords.add(record);
        }
        return orderedRecords;
    }

    private void ensureOwnedBy(ExportRecord record, Long operatorId) {
        record.requireOwnedBy(operatorId);
    }

    private void ensureDownloadable(ExportRecord record) {
        record.requireBatchDownloadable();
    }

    private void assertTotalFileSizeWithinLimit(List<ExportRecord> records) {
        long totalFileSize = 0L;
        for (ExportRecord record : records) {
            Long fileSize = record.getFileSize();
            if (fileSize > MAX_BATCH_TOTAL_BYTES || totalFileSize > MAX_BATCH_TOTAL_BYTES - fileSize) {
                throw new BizException(ExportCenterErrorCode.EXPORT_BATCH_FILE_SIZE_LIMIT_EXCEEDED);
            }
            totalFileSize += fileSize;
        }
    }

    private String buildBatchFileName() {
        return BATCH_EXPORT_BIZ_NAME + "-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".zip";
    }

    private RenderedExportFile renderZipFile(String fileName, List<BatchSourceFile> sourceFiles) throws IOException {
        Path contentPath = Files.createTempFile("oig-export-batch-", ".zip");
        boolean completed = false;
        try {
            writeZip(contentPath, sourceFiles);
            RenderedExportFile file = new RenderedExportFile();
            file.setFileName(fileName);
            file.setFileType(BATCH_FILE_TYPE);
            file.setContentType(BATCH_CONTENT_TYPE);
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

    private void writeZip(Path contentPath, List<BatchSourceFile> sourceFiles) throws IOException {
        Map<String, Integer> entryNameCounts = new LinkedHashMap<>();
        long totalSourceBytes = 0L;
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(contentPath),
                StandardCharsets.UTF_8
        )) {
            for (BatchSourceFile sourceFile : sourceFiles) {
                ZipEntry zipEntry = new ZipEntry(resolveUniqueEntryName(sourceFile.fileName(), entryNameCounts));
                zipOutputStream.putNextEntry(zipEntry);
                try (InputStream inputStream = exportFileAccessor.openStream(sourceFile.objectKey())) {
                    if (inputStream == null) {
                        throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_DOWNLOADABLE);
                    }
                    totalSourceBytes = copyWithinLimit(inputStream, zipOutputStream, totalSourceBytes);
                }
                zipOutputStream.closeEntry();
            }
        }
    }

    private long copyWithinLimit(InputStream inputStream, ZipOutputStream outputStream, long totalSourceBytes)
            throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        int read;
        long copied = totalSourceBytes;
        while ((read = inputStream.read(buffer)) >= 0) {
            if (read == 0) {
                continue;
            }
            if (copied > MAX_BATCH_TOTAL_BYTES - read) {
                throw new BizException(ExportCenterErrorCode.EXPORT_BATCH_FILE_SIZE_LIMIT_EXCEEDED);
            }
            outputStream.write(buffer, 0, read);
            copied += read;
        }
        return copied;
    }

    private String resolveUniqueEntryName(String fileName, Map<String, Integer> entryNameCounts) {
        String entryName = sanitizeEntryName(fileName);
        int count = entryNameCounts.merge(entryName, 1, Integer::sum);
        if (count == 1) {
            return entryName;
        }
        return appendDuplicateIndex(entryName, count);
    }

    private String sanitizeEntryName(String fileName) {
        String normalized = StringUtils.hasText(fileName) ? fileName.trim() : "导出文件";
        normalized = normalized
                .replace('\\', '-')
                .replace('/', '-')
                .replaceAll("[\\r\\n\\t\\x00-\\x1F]+", "-");
        return StringUtils.hasText(normalized) ? normalized : "导出文件";
    }

    private String appendDuplicateIndex(String entryName, int count) {
        int extensionIndex = entryName.lastIndexOf('.');
        if (extensionIndex <= 0 || extensionIndex == entryName.length() - 1) {
            return entryName + "(" + count + ")";
        }
        return entryName.substring(0, extensionIndex)
                + "("
                + count
                + ")"
                + entryName.substring(extensionIndex);
    }

    private void handleTaskFailure(
            Long taskRecordId,
            ExportStoredFile storedFile,
            String failCode,
            String failMessage,
            Exception cause
    ) {
        cleanupStoredFileQuietly(storedFile);
        markFailedQuietly(taskRecordId, failCode, failMessage);
        log.warn("batch export task failed, recordId={}", taskRecordId, cause);
    }

    private void markFailedQuietly(Long recordId, String failCode, String failMessage) {
        try {
            exportRecordLifecycleAppService.markFailed(
                    recordId,
                    truncate(failCode, 64),
                    truncate(failMessage, MAX_FAIL_MESSAGE_LENGTH)
            );
        } catch (Exception ex) {
            log.error("failed to persist batch export task failure, recordId={}", recordId, ex);
        }
    }

    private void cleanupStoredFileQuietly(ExportStoredFile storedFile) {
        if (storedFile == null || !StringUtils.hasText(storedFile.getObjectKey())) {
            return;
        }
        try {
            exportFileSink.delete(storedFile.getObjectKey());
        } catch (Exception ex) {
            log.warn("failed to clean uploaded batch export object, objectKey={}", storedFile.getObjectKey(), ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record BatchSourceFile(Long recordId, String fileName, String objectKey) {
    }
}
