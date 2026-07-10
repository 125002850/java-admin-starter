package com.example.admin.export.service;

import com.example.admin.core.exception.BizException;
import com.example.admin.core.export.model.ExportStoreRequest;
import com.example.admin.core.export.model.ExportStoredFile;
import com.example.admin.core.export.model.RenderedExportFile;
import com.example.admin.core.export.spi.ExportFileAccessor;
import com.example.admin.core.export.spi.ExportFileSink;
import com.example.admin.export.enums.ExportCenterErrorCode;
import com.example.admin.export.enums.ExportRecordStatus;
import com.example.admin.export.infra.entity.ExportRecordEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
public class ExportBatchDownloadService {

    private static final int MAX_BATCH_RECORD_COUNT = 50;
    private static final long MAX_BATCH_TOTAL_BYTES = 200L * 1024 * 1024;
    private static final String BATCH_EXPORT_BIZ_NAME = "导出中心批量下载";
    private static final String BATCH_FILE_TYPE = "zip";
    private static final String BATCH_CONTENT_TYPE = "application/zip";
    private static final String BATCH_BIZ_PATH = "export/mdm_export_center_batch_download";
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExportRecordService exportRecordService;
    private final ExportFileAccessor exportFileAccessor;
    private final ExportFileSink exportFileSink;

    public ExportBatchDownloadService(
            ExportRecordService exportRecordService,
            ExportFileAccessor exportFileAccessor,
            ExportFileSink exportFileSink
    ) {
        this.exportRecordService = exportRecordService;
        this.exportFileAccessor = exportFileAccessor;
        this.exportFileSink = exportFileSink;
    }

    public BatchDownloadedFile packageRecords(List<Long> recordIds, Long operatorId) {
        List<Long> normalizedRecordIds = normalizeRecordIds(recordIds);
        List<ExportRecordEntity> records = listOwnedDownloadableRecords(normalizedRecordIds, operatorId);
        assertTotalFileSizeWithinLimit(records);

        String fileName = buildBatchFileName();
        try {
            RenderedExportFile zipFile = renderZipFile(fileName, records);
            ExportStoreRequest storeRequest = new ExportStoreRequest();
            storeRequest.setBizPath(BATCH_BIZ_PATH);
            ExportStoredFile storedFile = exportFileSink.store(zipFile, storeRequest);
            String downloadUrl = exportFileAccessor.fetchTempUrl(storedFile.getObjectKey());
            for (ExportRecordEntity record : records) {
                exportRecordService.recordDownloadLinkAcquired(record.getId(), operatorId);
            }
            return new BatchDownloadedFile(
                    fileName,
                    downloadUrl,
                    storedFile.getObjectKey(),
                    storedFile.getContentType(),
                    storedFile.getFileSize()
            );
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ExportCenterErrorCode.EXPORT_EXECUTION_FAILED);
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

    private List<ExportRecordEntity> listOwnedDownloadableRecords(List<Long> recordIds, Long operatorId) {
        List<ExportRecordEntity> records = exportRecordService.listActiveByIds(recordIds);
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
            ensureOwnedBy(record, operatorId);
            ensureDownloadable(record);
            orderedRecords.add(record);
        }
        return orderedRecords;
    }

    private void ensureOwnedBy(ExportRecordEntity record, Long operatorId) {
        Long ownerId = record.getCreateBy() == null ? 0L : record.getCreateBy();
        Long currentOperatorId = operatorId == null ? 0L : operatorId;
        if (!ownerId.equals(currentOperatorId)) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_FORBIDDEN);
        }
    }

    private void ensureDownloadable(ExportRecordEntity record) {
        if (record.getStatus() == null
                || !record.getStatus().equals(ExportRecordStatus.SUCCESS.getIntCode())
                || !StringUtils.hasText(record.getObjectKey())
                || !StringUtils.hasText(record.getFileName())
                || record.getFileSize() == null
                || record.getFileSize() < 0
                || BATCH_FILE_TYPE.equalsIgnoreCase(record.getFileType())) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_DOWNLOADABLE);
        }
    }

    private void assertTotalFileSizeWithinLimit(List<ExportRecordEntity> records) {
        long totalFileSize = 0L;
        for (ExportRecordEntity record : records) {
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

    private RenderedExportFile renderZipFile(String fileName, List<ExportRecordEntity> records) throws IOException {
        byte[] content = zipRecords(records);
        RenderedExportFile file = new RenderedExportFile();
        file.setFileName(fileName);
        file.setFileType(BATCH_FILE_TYPE);
        file.setContentType(BATCH_CONTENT_TYPE);
        file.setContent(content);
        file.setFileSize(content.length);
        return file;
    }

    private byte[] zipRecords(List<ExportRecordEntity> records) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<String, Integer> entryNameCounts = new LinkedHashMap<>();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, java.nio.charset.StandardCharsets.UTF_8)) {
            for (ExportRecordEntity record : records) {
                byte[] sourceContent = exportFileAccessor.fetchContent(record.getObjectKey());
                if (sourceContent == null) {
                    throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_DOWNLOADABLE);
                }
                ZipEntry zipEntry = new ZipEntry(resolveUniqueEntryName(record.getFileName(), entryNameCounts));
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(sourceContent);
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
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

    public record BatchDownloadedFile(
            String fileName,
            String downloadUrl,
            String objectKey,
            String contentType,
            Long fileSize
    ) {
    }
}
