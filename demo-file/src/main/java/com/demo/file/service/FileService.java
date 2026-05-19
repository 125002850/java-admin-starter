package com.demo.file.service;

import com.demo.core.exception.BizException;
import com.demo.file.config.FileStorageProperties;
import com.demo.file.enums.FileErrorCode;
import com.demo.file.infra.provider.FileStorageProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileService {

    private static final Pattern BIZ_PATH_SEGMENT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern OBJECT_KEY_SEGMENT_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileStorageProvider fileStorageProvider;
    private final FileStorageProperties fileStorageProperties;

    public FileService(FileStorageProvider fileStorageProvider, FileStorageProperties fileStorageProperties) {
        this.fileStorageProvider = fileStorageProvider;
        this.fileStorageProperties = fileStorageProperties;
    }

    public StoredFile upload(MultipartFile file, String bizPath, String objectKey) {
        if (file == null || file.isEmpty()) {
            throw new BizException(FileErrorCode.EMPTY_FILE);
        }
        String normalizedBizPath = normalizeBizPath(bizPath);
        String resolvedObjectKey = StringUtils.hasText(objectKey)
                ? normalizeObjectKey(objectKey)
                : generateObjectKey(normalizedBizPath, file.getOriginalFilename());

        try (var inputStream = file.getInputStream()) {
            return fileStorageProvider.upload(
                    inputStream,
                    resolvedObjectKey,
                    file.getContentType(),
                    file.getSize(),
                    file.getOriginalFilename()
            );
        } catch (IOException ex) {
            throw new BizException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void delete(String objectKey) {
        fileStorageProvider.delete(normalizeObjectKey(objectKey));
    }

    public String fetchTempUrl(String objectKey) {
        try {
            return fileStorageProvider.buildTempUrl(normalizeObjectKey(objectKey));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.TEMP_URL_GENERATE_FAILED);
        }
    }

    private String generateObjectKey(String bizPath, String originalFilename) {
        String extension = resolveExtension(originalFilename);
        return bizPath + "/"
                + resolveCurrentDatePath() + "/"
                + UUID.randomUUID()
                + extension;
    }

    private String resolveCurrentDatePath() {
        try {
            return LocalDate.now(ZoneId.of(fileStorageProperties.getZoneId())).format(DATE_FORMATTER);
        } catch (DateTimeException ex) {
            throw new BizException(FileErrorCode.INVALID_STORAGE_CONFIG);
        }
    }

    private String normalizeBizPath(String bizPath) {
        if (!StringUtils.hasText(bizPath)) {
            throw new BizException(FileErrorCode.INVALID_BIZ_PATH);
        }
        String[] segments = bizPath.split("/");
        List<String> normalizedSegments = new ArrayList<>();
        for (String segment : segments) {
            if (!StringUtils.hasText(segment) || ".".equals(segment) || "..".equals(segment)) {
                throw new BizException(FileErrorCode.INVALID_BIZ_PATH);
            }
            if (!BIZ_PATH_SEGMENT_PATTERN.matcher(segment).matches()) {
                throw new BizException(FileErrorCode.INVALID_BIZ_PATH);
            }
            normalizedSegments.add(segment);
        }
        return String.join("/", normalizedSegments);
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BizException(FileErrorCode.INVALID_OBJECT_KEY);
        }
        String[] segments = objectKey.split("/");
        List<String> normalizedSegments = new ArrayList<>();
        for (String segment : segments) {
            if (!StringUtils.hasText(segment) || ".".equals(segment) || "..".equals(segment)) {
                throw new BizException(FileErrorCode.INVALID_OBJECT_KEY);
            }
            if (!OBJECT_KEY_SEGMENT_PATTERN.matcher(segment).matches()) {
                throw new BizException(FileErrorCode.INVALID_OBJECT_KEY);
            }
            normalizedSegments.add(segment);
        }
        return String.join("/", normalizedSegments);
    }

    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(extensionIndex);
    }
}
