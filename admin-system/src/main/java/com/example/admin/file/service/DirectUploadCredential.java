package com.example.admin.file.service;

public class DirectUploadCredential {

    private final String provider;
    private final String credential;
    private final String objectKey;
    private final String originUrl;
    private final String uploadHost;

    public DirectUploadCredential(
            String provider,
            String credential,
            String objectKey,
            String originUrl,
            String uploadHost
    ) {
        this.provider = provider;
        this.credential = credential;
        this.objectKey = objectKey;
        this.originUrl = originUrl;
        this.uploadHost = uploadHost;
    }

    public String getProvider() {
        return provider;
    }

    public String getCredential() {
        return credential;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public String getUploadHost() {
        return uploadHost;
    }
}
