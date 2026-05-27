package com.demo.file.infra.provider.minio;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.demo.core.exception.BizException;
import com.demo.file.config.FileStorageProperties;
import com.demo.file.enums.FileErrorCode;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import okhttp3.OkHttpClient;

@Component
@ConditionalOnProperty(prefix = "platform.file.storage", name = "type", havingValue = "minio")
public class DefaultMinioOperations implements MinioOperations {

    private final MinioClient minioClient;

    public DefaultMinioOperations(FileStorageProperties fileStorageProperties) {
        FileStorageProperties.Minio minio = fileStorageProperties.getMinio();
        if (minio == null
                || !StringUtils.hasText(minio.getEndpoint())
                || !StringUtils.hasText(minio.getAccessKey())
                || !StringUtils.hasText(minio.getSecretKey())) {
            throw new BizException(FileErrorCode.INVALID_STORAGE_CONFIG);
        }

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .httpClient(buildHttpClient(minio));
        if (StringUtils.hasText(minio.getRegion())) {
            builder.region(minio.getRegion());
        }
        this.minioClient = builder.build();
    }

    @Override
    public void upload(InputStream inputStream, String bucketName, String objectKey, String contentType, long size)
            throws Exception {
        PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(inputStream, size, -1);
        if (StringUtils.hasText(contentType)) {
            builder.contentType(contentType);
        }
        minioClient.putObject(builder.build());
    }

    @Override
    public void delete(String bucketName, String objectKey) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build());
    }

    @Override
    public String createPrivateDownloadUrl(String bucketName, String objectKey, long expireSeconds) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(objectKey)
                .expiry(Math.toIntExact(expireSeconds), TimeUnit.SECONDS)
                .build());
    }

    private OkHttpClient buildHttpClient(FileStorageProperties.Minio minio) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(minio.getConnectTimeout()))
                .readTimeout(Duration.ofSeconds(minio.getReadTimeout()));
        if (minio.getWriteTimeout() > 0) {
            builder.writeTimeout(Duration.ofSeconds(minio.getWriteTimeout()));
        }
        return builder.build();
    }
}
