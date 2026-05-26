package com.demo.file;

import com.demo.core.exception.BizException;
import com.demo.file.config.FileStorageProperties;
import com.demo.file.infra.provider.qiniu.QiniuFileStorageProvider;
import com.demo.file.infra.provider.qiniu.QiniuOperations;
import com.demo.file.service.DirectUploadCredential;
import com.demo.file.service.StoredFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

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

class QiniuFileStorageProviderTests {

    @Test
    void fetchDirectUploadCredential_should_return_qiniu_payload() {
        QiniuOperations qiniuOperations = mock(QiniuOperations.class);
        when(qiniuOperations.createUploadToken("demo-bucket", "avatar/user/2026/05/19/test.png", 600L))
                .thenReturn("upload-token");

        QiniuFileStorageProvider provider = new QiniuFileStorageProvider(properties(true), qiniuOperations);

        DirectUploadCredential credential = provider.fetchDirectUploadCredential("avatar/user/2026/05/19/test.png");

        assertThat(credential.getProvider()).isEqualTo("qiniu");
        assertThat(credential.getCredential()).isEqualTo("upload-token");
        assertThat(credential.getObjectKey()).isEqualTo("avatar/user/2026/05/19/test.png");
        assertThat(credential.getOriginUrl()).isEqualTo("https://cdn.example.com/avatar/user/2026/05/19/test.png");
        assertThat(credential.getUploadHost()).isEqualTo("https://upload.qiniup.com");
    }

    @Test
    void upload_should_return_stored_file_when_qiniu_upload_succeeds() throws Exception {
        QiniuOperations qiniuOperations = mock(QiniuOperations.class);
        when(qiniuOperations.createUploadToken("demo-bucket", "avatar/user/2026/05/19/test.png", 600L))
                .thenReturn("upload-token");
        doNothing().when(qiniuOperations).upload(any(), eq("avatar/user/2026/05/19/test.png"), eq("upload-token"), eq("image/png"));

        QiniuFileStorageProvider provider = new QiniuFileStorageProvider(properties(false), qiniuOperations);

        StoredFile storedFile = provider.upload(
                new ByteArrayInputStream("qiniu".getBytes()),
                "avatar/user/2026/05/19/test.png",
                "image/png",
                5L,
                "test.png"
        );

        assertThat(storedFile.getObjectKey()).isEqualTo("avatar/user/2026/05/19/test.png");
        assertThat(storedFile.getOriginUrl()).isEqualTo("https://cdn.example.com/avatar/user/2026/05/19/test.png");
        assertThat(storedFile.getFileName()).isEqualTo("test.png");
        verify(qiniuOperations).upload(any(), eq("avatar/user/2026/05/19/test.png"), eq("upload-token"), eq("image/png"));
    }

    @Test
    void upload_should_translate_sdk_exception() throws Exception {
        QiniuOperations qiniuOperations = mock(QiniuOperations.class);
        when(qiniuOperations.createUploadToken("demo-bucket", "avatar/user/2026/05/19/test.png", 600L))
                .thenReturn("upload-token");
        doThrow(new RuntimeException("sdk upload failed"))
                .when(qiniuOperations)
                .upload(any(), eq("avatar/user/2026/05/19/test.png"), eq("upload-token"), eq("image/png"));

        QiniuFileStorageProvider provider = new QiniuFileStorageProvider(properties(false), qiniuOperations);

        assertThatThrownBy(() -> provider.upload(
                new ByteArrayInputStream("qiniu".getBytes()),
                "avatar/user/2026/05/19/test.png",
                "image/png",
                5L,
                "test.png"
        )).isInstanceOf(BizException.class)
                .hasMessage("文件上传失败");
    }

    @Test
    void fetchTempUrl_should_return_origin_url_when_bucket_is_public() {
        QiniuOperations qiniuOperations = mock(QiniuOperations.class);
        QiniuFileStorageProvider provider = new QiniuFileStorageProvider(properties(false), qiniuOperations);

        String tempUrl = provider.buildTempUrl("avatar/user/2026/05/19/test.png");

        assertThat(tempUrl).isEqualTo("https://cdn.example.com/avatar/user/2026/05/19/test.png");
    }

    @Test
    void fetchTempUrl_should_create_private_download_url_when_bucket_is_private() {
        QiniuOperations qiniuOperations = mock(QiniuOperations.class);
        when(qiniuOperations.createPrivateDownloadUrl("https://cdn.example.com/avatar/user/2026/05/19/test.png", 1200L))
                .thenReturn("https://cdn.example.com/avatar/user/2026/05/19/test.png?e=123");
        QiniuFileStorageProvider provider = new QiniuFileStorageProvider(properties(true), qiniuOperations);

        String tempUrl = provider.buildTempUrl("avatar/user/2026/05/19/test.png");

        assertThat(tempUrl).isEqualTo("https://cdn.example.com/avatar/user/2026/05/19/test.png?e=123");
    }

    @Test
    void delete_should_translate_sdk_exception() throws Exception {
        QiniuOperations qiniuOperations = mock(QiniuOperations.class);
        doThrow(new RuntimeException("sdk delete failed"))
                .when(qiniuOperations)
                .delete("demo-bucket", "avatar/user/2026/05/19/test.png");

        QiniuFileStorageProvider provider = new QiniuFileStorageProvider(properties(false), qiniuOperations);

        assertThatThrownBy(() -> provider.delete("avatar/user/2026/05/19/test.png"))
                .isInstanceOf(BizException.class)
                .hasMessage("文件删除失败");
    }

    private FileStorageProperties properties(boolean privateBucket) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setType("qiniu");
        properties.setZoneId("Asia/Shanghai");

        FileStorageProperties.Qiniu qiniu = new FileStorageProperties.Qiniu();
        qiniu.setAccessKey("access-key");
        qiniu.setSecretKey("secret-key");
        qiniu.setBucketName("demo-bucket");
        qiniu.setBaseUrl("https://cdn.example.com");
        qiniu.setUploadHost("https://upload.qiniup.com");
        qiniu.setPrivateBucket(privateBucket);
        qiniu.setUploadTokenExpireSeconds(600L);
        qiniu.setDownloadUrlExpireSeconds(1200L);
        qiniu.setRegion("huadong");
        properties.setQiniu(qiniu);
        return properties;
    }
}
