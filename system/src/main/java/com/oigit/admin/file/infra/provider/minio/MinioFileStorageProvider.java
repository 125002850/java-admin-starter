package com.oigit.admin.file.infra.provider.minio;

import java.io.InputStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.file.config.FileStorageProperties;
import com.oigit.admin.file.enums.FileErrorCode;
import com.oigit.admin.file.infra.provider.FileStorageProvider;
import com.oigit.admin.file.service.StoredFile;

@Component
@ConditionalOnProperty(prefix = "platform.file.storage", name = "type", havingValue = "minio")
public class MinioFileStorageProvider implements FileStorageProvider {

    private final MinioOperations minioOperations;
    private final String bucketName;
    private final String baseUrl;
    private final boolean privateBucket;
    private final long downloadUrlExpireSeconds;

    public MinioFileStorageProvider(FileStorageProperties fileStorageProperties, MinioOperations minioOperations) {
        FileStorageProperties.Minio minio = fileStorageProperties.getMinio();
        if (minio == null
                || !StringUtils.hasText(minio.getBucketName())
                || !StringUtils.hasText(minio.getBaseUrl())
                || !StringUtils.hasText(minio.getEndpoint())
                || !StringUtils.hasText(minio.getAccessKey())
                || !StringUtils.hasText(minio.getSecretKey())) {
            throw new BizException(FileErrorCode.INVALID_STORAGE_CONFIG);
        }
        this.minioOperations = minioOperations;
        this.bucketName = minio.getBucketName();
        this.baseUrl = normalizeBaseUrl(minio.getBaseUrl());
        this.privateBucket = minio.isPrivateBucket();
        this.downloadUrlExpireSeconds = minio.getDownloadUrlExpireSeconds();
    }

    @Override
    public StoredFile upload(InputStream inputStream, String objectKey, String contentType, long size, String fileName) {
        try {
            minioOperations.upload(inputStream, bucketName, objectKey, contentType, size);
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
        return new StoredFile(objectKey, buildOriginUrl(objectKey), fileName, contentType, size);
    }

    @Override
    public InputStream openStream(String objectKey) {
        try {
            return minioOperations.download(bucketName, objectKey);
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioOperations.delete(bucketName, objectKey);
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.FILE_DELETE_FAILED);
        }
    }

    @Override
    public String buildOriginUrl(String objectKey) {
        return baseUrl + "/" + objectKey;
    }

    @Override
    public String buildTempUrl(String objectKey) {
        if (!privateBucket) {
            return buildOriginUrl(objectKey);
        }
        try {
            return minioOperations.createPrivateDownloadUrl(bucketName, objectKey, downloadUrlExpireSeconds);
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.TEMP_URL_GENERATE_FAILED);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
