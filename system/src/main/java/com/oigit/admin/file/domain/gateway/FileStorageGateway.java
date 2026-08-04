package com.oigit.admin.file.domain.gateway;

import com.oigit.admin.file.domain.model.DirectUploadCredential;
import com.oigit.admin.file.domain.model.StoredFile;

import java.io.InputStream;
import java.util.Optional;

public interface FileStorageGateway {

    StoredFile upload(InputStream inputStream, String objectKey, String contentType, long size, String fileName);

    InputStream openStream(String objectKey);

    void delete(String objectKey);

    String buildOriginUrl(String objectKey);

    String buildTempUrl(String objectKey);

    default Optional<DirectUploadCredential> fetchDirectUploadCredential(String objectKey) {
        return Optional.empty();
    }
}
