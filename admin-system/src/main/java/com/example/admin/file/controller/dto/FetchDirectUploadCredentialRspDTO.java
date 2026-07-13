package com.example.admin.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "直传凭证响应")
public class FetchDirectUploadCredentialRspDTO {

    @Schema(description = "provider 标识", example = "qiniu")
    private String provider;

    @Schema(description = "直传凭证", example = "stub-credential")
    private String credential;

    @Schema(description = "对象键", example = "avatar/user/2026/05/19/example.png")
    private String objectKey;

    @Schema(description = "原始访问地址", example = "/local-files/avatar/user/2026/05/19/example.png")
    private String originUrl;

    @Schema(description = "上传域名", example = "https://upload.qiniup.com")
    private String uploadHost;

    public FetchDirectUploadCredentialRspDTO() {
    }

    public FetchDirectUploadCredentialRspDTO(
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

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getUploadHost() {
        return uploadHost;
    }

    public void setUploadHost(String uploadHost) {
        this.uploadHost = uploadHost;
    }
}
