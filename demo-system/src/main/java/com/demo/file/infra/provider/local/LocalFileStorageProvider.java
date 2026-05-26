package com.demo.file.infra.provider.local;

import com.demo.core.exception.BizException;
import com.demo.file.config.FileStorageProperties;
import com.demo.file.enums.FileErrorCode;
import com.demo.file.infra.provider.FileStorageProvider;
import com.demo.file.service.StoredFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "platform.file.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageProvider implements FileStorageProvider {

    private static final Pattern OBJECT_KEY_SEGMENT_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final Path rootDir;
    private final String baseUrl;

    public LocalFileStorageProvider(FileStorageProperties fileStorageProperties) {
        FileStorageProperties.Local local = fileStorageProperties.getLocal();
        if (local == null || !StringUtils.hasText(local.getRootDir()) || !StringUtils.hasText(local.getBaseUrl())) {
            throw new BizException(FileErrorCode.INVALID_STORAGE_CONFIG);
        }
        this.rootDir = Path.of(local.getRootDir()).toAbsolutePath().normalize();
        this.baseUrl = normalizeBaseUrl(local.getBaseUrl());
    }

    @Override
    public StoredFile upload(InputStream inputStream, String objectKey, String contentType, long size, String fileName) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        Path targetPath = resolvePath(normalizedObjectKey);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BizException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
        return new StoredFile(
                normalizedObjectKey,
                buildOriginUrl(normalizedObjectKey),
                fileName,
                contentType,
                size
        );
    }

    @Override
    public void delete(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        Path targetPath = resolvePath(normalizedObjectKey);
        if (Files.notExists(targetPath)) {
            throw new BizException(FileErrorCode.FILE_NOT_FOUND);
        }
        try {
            Files.delete(targetPath);
        } catch (IOException ex) {
            throw new BizException(FileErrorCode.FILE_DELETE_FAILED);
        }
    }

    @Override
    public String buildOriginUrl(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        return baseUrl + "/" + normalizedObjectKey;
    }

    @Override
    public String buildTempUrl(String objectKey) {
        return buildOriginUrl(objectKey);
    }

    private Path resolvePath(String objectKey) {
        Path resolved = rootDir.resolve(objectKey).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new BizException(FileErrorCode.INVALID_OBJECT_KEY);
        }
        return resolved;
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

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.startsWith("/") ? baseUrl : "/" + baseUrl;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
