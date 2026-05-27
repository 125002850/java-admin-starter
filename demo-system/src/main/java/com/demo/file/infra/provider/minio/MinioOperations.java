package com.demo.file.infra.provider.minio;

import java.io.InputStream;

public interface MinioOperations {

    void upload(InputStream inputStream, String bucketName, String objectKey, String contentType, long size) throws Exception;

    void delete(String bucketName, String objectKey) throws Exception;

    String createPrivateDownloadUrl(String bucketName, String objectKey, long expireSeconds) throws Exception;
}
