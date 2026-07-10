package com.example.admin.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "已存储文件响应")
public class StoredFileRspDTO {

    @Schema(description = "对象键", example = "avatar/user/2026/05/19/example.png")
    private String objectKey;

    @Schema(description = "原始访问地址", example = "/local-files/avatar/user/2026/05/19/example.png")
    private String originUrl;

    @Schema(description = "原始文件名", example = "avatar.png")
    private String fileName;

    @Schema(description = "文件内容类型", example = "image/png")
    private String contentType;

    @Schema(description = "文件大小，单位字节", example = "1024")
    private long size;

    public StoredFileRspDTO() {
    }

    public StoredFileRspDTO(String objectKey, String originUrl, String fileName, String contentType, long size) {
        this.objectKey = objectKey;
        this.originUrl = originUrl;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
