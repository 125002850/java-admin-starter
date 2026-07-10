package com.example.admin.file.infra.provider;

import com.example.admin.file.service.StoredFile;

import java.io.InputStream;

public interface FileStorageProvider {

    StoredFile upload(InputStream inputStream, String objectKey, String contentType, long size, String fileName);

    byte[] download(String objectKey);

    void delete(String objectKey);

    String buildOriginUrl(String objectKey);

    String buildTempUrl(String objectKey);
}
