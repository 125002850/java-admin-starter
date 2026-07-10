package com.example.admin.file;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import com.example.admin.core.exception.BizException;
import com.example.admin.file.config.FileStorageProperties;
import com.example.admin.file.infra.provider.minio.MinioFileStorageProvider;
import com.example.admin.file.infra.provider.minio.MinioOperations;
import com.example.admin.file.service.StoredFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioFileStorageProviderTests {

    @Test
    void upload_should_return_stored_file_when_minio_upload_succeeds() throws Exception {
        MinioOperations minioOperations = mock(MinioOperations.class);
        doNothing().when(minioOperations)
                .upload(any(), eq("admin-bucket"), eq("avatar/user/2026/05/19/test.png"), eq("image/png"), eq(5L));

        MinioFileStorageProvider provider = new MinioFileStorageProvider(properties(false), minioOperations);

        StoredFile storedFile = provider.upload(
                new ByteArrayInputStream("minio".getBytes()),
                "avatar/user/2026/05/19/test.png",
                "image/png",
                5L,
                "test.png"
        );

        assertThat(storedFile.getObjectKey()).isEqualTo("avatar/user/2026/05/19/test.png");
        assertThat(storedFile.getOriginUrl()).isEqualTo("https://cdn.example.com/avatar/user/2026/05/19/test.png");
        assertThat(storedFile.getFileName()).isEqualTo("test.png");
        verify(minioOperations).upload(any(), eq("admin-bucket"), eq("avatar/user/2026/05/19/test.png"), eq("image/png"), eq(5L));
    }

    @Test
    void upload_should_translate_sdk_exception() throws Exception {
        MinioOperations minioOperations = mock(MinioOperations.class);
        doThrow(new RuntimeException("sdk upload failed"))
                .when(minioOperations)
                .upload(any(), eq("admin-bucket"), eq("avatar/user/2026/05/19/test.png"), eq("image/png"), eq(5L));

        MinioFileStorageProvider provider = new MinioFileStorageProvider(properties(false), minioOperations);

        assertThatThrownBy(() -> provider.upload(
                new ByteArrayInputStream("minio".getBytes()),
                "avatar/user/2026/05/19/test.png",
                "image/png",
                5L,
                "test.png"
        )).isInstanceOf(BizException.class)
                .hasMessage("文件上传失败");
    }

    @Test
    void fetchTempUrl_should_return_origin_url_when_bucket_is_public() {
        MinioOperations minioOperations = mock(MinioOperations.class);
        MinioFileStorageProvider provider = new MinioFileStorageProvider(properties(false), minioOperations);

        String tempUrl = provider.buildTempUrl("avatar/user/2026/05/19/test.png");

        assertThat(tempUrl).isEqualTo("https://cdn.example.com/avatar/user/2026/05/19/test.png");
    }

    @Test
    void fetchTempUrl_should_create_private_download_url_when_bucket_is_private() throws Exception {
        MinioOperations minioOperations = mock(MinioOperations.class);
        when(minioOperations.createPrivateDownloadUrl("admin-bucket", "avatar/user/2026/05/19/test.png", 1200L))
                .thenReturn("https://minio.example.com/admin-bucket/avatar/user/2026/05/19/test.png?X-Amz-Signature=abc");
        MinioFileStorageProvider provider = new MinioFileStorageProvider(properties(true), minioOperations);

        String tempUrl = provider.buildTempUrl("avatar/user/2026/05/19/test.png");

        assertThat(tempUrl).contains("X-Amz-Signature=");
    }

    @Test
    void delete_should_translate_sdk_exception() throws Exception {
        MinioOperations minioOperations = mock(MinioOperations.class);
        doThrow(new RuntimeException("sdk delete failed"))
                .when(minioOperations)
                .delete("admin-bucket", "avatar/user/2026/05/19/test.png");

        MinioFileStorageProvider provider = new MinioFileStorageProvider(properties(false), minioOperations);

        assertThatThrownBy(() -> provider.delete("avatar/user/2026/05/19/test.png"))
                .isInstanceOf(BizException.class)
                .hasMessage("文件删除失败");
    }

    private FileStorageProperties properties(boolean privateBucket) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setType("minio");
        properties.setZoneId("Asia/Shanghai");

        FileStorageProperties.Minio minio = new FileStorageProperties.Minio();
        minio.setEndpoint("https://minio.example.com");
        minio.setAccessKey("access-key");
        minio.setSecretKey("secret-key");
        minio.setBucketName("admin-bucket");
        minio.setBaseUrl("https://cdn.example.com");
        minio.setPrivateBucket(privateBucket);
        minio.setDownloadUrlExpireSeconds(1200L);
        minio.setRegion("us-east-1");
        properties.setMinio(minio);
        return properties;
    }
}
