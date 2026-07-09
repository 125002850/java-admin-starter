package com.demo.export.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量下载导出记录响应")
public class ExportBatchDownloadRspDTO {

    @Schema(description = "打包文件名", example = "导出中心批量下载-20260629114000.zip")
    private String fileName;

    @Schema(description = "临时下载地址", example = "https://example.com/download/export-batch.zip")
    private String downloadUrl;

    @Schema(description = "内容类型", example = "application/zip")
    private String contentType;

    @Schema(description = "文件大小，单位字节", example = "2048")
    private Long fileSize;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}
