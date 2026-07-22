package com.oigit.admin.file.service;

public class StoredFile {

    private final String objectKey;
    private final String originUrl;
    private final String fileName;
    private final String contentType;
    private final long size;

    public StoredFile(String objectKey, String originUrl, String fileName, String contentType, long size) {
        this.objectKey = objectKey;
        this.originUrl = originUrl;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }
}
