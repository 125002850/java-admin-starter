package com.oigit.admin.file.domain.service;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.file.enums.FileErrorCode;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** 文件业务路径与对象键的领域规则。 */
public class FileObjectKeyPolicy {

    private static final Pattern BIZ_PATH_SEGMENT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern OBJECT_KEY_SEGMENT_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final ZoneId zoneId;
    private final Clock clock;

    public FileObjectKeyPolicy(ZoneId zoneId, Clock clock) {
        this.zoneId = zoneId;
        this.clock = clock;
    }

    public String resolveObjectKey(String bizPath, String objectKey, String originalFilename) {
        String normalizedBizPath = normalizeBizPath(bizPath);
        if (objectKey != null && !objectKey.isBlank()) {
            return normalizeObjectKey(objectKey);
        }
        return normalizedBizPath + "/"
                + LocalDate.now(clock).format(DATE_FORMATTER) + "/"
                + UUID.randomUUID()
                + resolveExtension(originalFilename);
    }

    public String normalizeObjectKey(String objectKey) {
        return normalizeSegments(objectKey, OBJECT_KEY_SEGMENT_PATTERN, FileErrorCode.INVALID_OBJECT_KEY);
    }

    private String normalizeBizPath(String bizPath) {
        return normalizeSegments(bizPath, BIZ_PATH_SEGMENT_PATTERN, FileErrorCode.INVALID_BIZ_PATH);
    }

    private String normalizeSegments(String value, Pattern pattern, FileErrorCode errorCode) {
        if (value == null || value.isBlank()) {
            throw new BizException(errorCode);
        }
        String[] segments = value.split("/");
        List<String> normalizedSegments = new ArrayList<>();
        for (String segment : segments) {
            if (segment == null || segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new BizException(errorCode);
            }
            if (!pattern.matcher(segment).matches()) {
                throw new BizException(errorCode);
            }
            normalizedSegments.add(segment);
        }
        return String.join("/", normalizedSegments);
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == originalFilename.length() - 1) {
            return "";
        }
        String extension = originalFilename.substring(extensionIndex);
        if (!OBJECT_KEY_SEGMENT_PATTERN.matcher("file" + extension).matches()) {
            throw new BizException(FileErrorCode.INVALID_OBJECT_KEY);
        }
        return extension;
    }

    public static ZoneId resolveZoneId(String configuredZoneId) {
        try {
            return ZoneId.of(configuredZoneId);
        } catch (DateTimeException | NullPointerException ex) {
            throw new BizException(FileErrorCode.INVALID_STORAGE_CONFIG);
        }
    }
}
