package com.example.admin.file.infra.provider.qiniu;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.admin.core.exception.BizException;
import com.example.admin.file.config.FileStorageProperties;
import com.example.admin.file.enums.FileErrorCode;
import com.example.admin.file.infra.provider.DirectUploadCapable;
import com.example.admin.file.infra.provider.FileStorageProvider;
import com.example.admin.file.service.DirectUploadCredential;
import com.example.admin.file.service.StoredFile;

@Component
@ConditionalOnProperty(prefix = "platform.file.storage", name = "type", havingValue = "qiniu")
public class QiniuFileStorageProvider implements FileStorageProvider, DirectUploadCapable {

    private static final String PROVIDER = "qiniu";

    private final QiniuOperations qiniuOperations;
    private final String bucketName;
    private final String baseUrl;
    private final String uploadHost;
    private final boolean privateBucket;
    private final long uploadTokenExpireSeconds;
    private final long downloadUrlExpireSeconds;

    public QiniuFileStorageProvider(FileStorageProperties fileStorageProperties, QiniuOperations qiniuOperations) {
        FileStorageProperties.Qiniu qiniu = fileStorageProperties.getQiniu();
        if (qiniu == null
                || !StringUtils.hasText(qiniu.getBucketName())
                || !StringUtils.hasText(qiniu.getBaseUrl())
                || !StringUtils.hasText(qiniu.getUploadHost())) {
            throw new BizException(FileErrorCode.INVALID_STORAGE_CONFIG);
        }
        this.qiniuOperations = qiniuOperations;
        this.bucketName = qiniu.getBucketName();
        this.baseUrl = normalizeBaseUrl(qiniu.getBaseUrl());
        this.uploadHost = qiniu.getUploadHost();
        this.privateBucket = qiniu.isPrivateBucket();
        this.uploadTokenExpireSeconds = qiniu.getUploadTokenExpireSeconds();
        this.downloadUrlExpireSeconds = qiniu.getDownloadUrlExpireSeconds();
    }

    @Override
    public StoredFile upload(InputStream inputStream, String objectKey, String contentType, long size, String fileName) {
        String uploadToken = qiniuOperations.createUploadToken(bucketName, objectKey, uploadTokenExpireSeconds);
        try {
            qiniuOperations.upload(inputStream, objectKey, uploadToken, contentType);
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
        return new StoredFile(objectKey, buildOriginUrl(objectKey), fileName, contentType, size);
    }

    @Override
    public byte[] download(String objectKey) {
        try {
            URLConnection connection = URI.create(buildTempUrl(objectKey)).toURL().openConnection();
            try (InputStream inputStream = connection.getInputStream()) {
                return inputStream.readAllBytes();
            }
        } catch (IllegalArgumentException | IOException ex) {
            throw new BizException(FileErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            qiniuOperations.delete(bucketName, objectKey);
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
        String originUrl = buildOriginUrl(objectKey);
        if (!privateBucket) {
            return originUrl;
        }
        try {
            return qiniuOperations.createPrivateDownloadUrl(originUrl, downloadUrlExpireSeconds);
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.TEMP_URL_GENERATE_FAILED);
        }
    }

    @Override
    public DirectUploadCredential fetchDirectUploadCredential(String objectKey) {
        try {
            String credential = qiniuOperations.createUploadToken(bucketName, objectKey, uploadTokenExpireSeconds);
            return new DirectUploadCredential(
                    PROVIDER,
                    credential,
                    objectKey,
                    buildOriginUrl(objectKey),
                    uploadHost
            );
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.DIRECT_UPLOAD_CREDENTIAL_GENERATE_FAILED);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
