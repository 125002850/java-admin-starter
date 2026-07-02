package com.demo.core.export.support;

import com.demo.core.export.model.ExportMeta;
import com.demo.core.export.model.ExportScope;
import com.demo.core.export.spi.ExportHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractCsvListExportHandler<Q> implements ExportHandler<Q> {

    private static final String FILE_TYPE = "csv";
    private static final int DEFAULT_EXPIRE_SECONDS = 3600;
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String sceneName() {
        return businessName() + "列表导出";
    }

    @Override
    public void validate(Q query) {
    }

    @Override
    public final ExportMeta buildMeta(Q query) {
        ExportScope scope = resolveExportScope(query);
        ExportMeta meta = new ExportMeta();
        meta.setFileType(FILE_TYPE);
        meta.setFileName(buildFileName(scope));
        meta.setExpireSeconds(resolveExpireSeconds());
        meta.setQuerySnapshotSummary(scope.getSummary());
        return meta;
    }

    protected abstract String businessName();

    protected ExportScope resolveExportScope(Q query) {
        return ExportScope.allData();
    }

    protected int resolveExpireSeconds() {
        return DEFAULT_EXPIRE_SECONDS;
    }

    protected LocalDateTime currentTime() {
        return LocalDateTime.now();
    }

    private String buildFileName(ExportScope scope) {
        String fileName = normalizeFileNamePart(businessName())
                + "-"
                + normalizeFileNamePart(scope.getFileNamePart())
                + "-"
                + currentTime().format(FILE_TIME_FORMATTER)
                + "."
                + FILE_TYPE;
        return fileName;
    }

    private String normalizeFileNamePart(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "导出";
        }
        return value.trim()
                .replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]+", "-")
                .replaceAll("\\s+", "");
    }
}
