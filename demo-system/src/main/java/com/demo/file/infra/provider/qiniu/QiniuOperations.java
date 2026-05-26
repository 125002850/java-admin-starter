package com.demo.file.infra.provider.qiniu;

import com.qiniu.common.QiniuException;

import java.io.InputStream;

public interface QiniuOperations {

    void upload(InputStream inputStream, String objectKey, String uploadToken, String contentType) throws QiniuException;

    void delete(String bucketName, String objectKey) throws QiniuException;

    String createUploadToken(String bucketName, String objectKey, long expireSeconds);

    String createPrivateDownloadUrl(String originUrl, long expireSeconds);
}
