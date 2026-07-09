package com.demo.export.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "导出下载响应")
public class ExportDownloadRspDTO {

    @Schema(description = "导出记录ID", example = "1")
    private Long recordId;

    @Schema(description = "文件名", example = "全局字典类型-筛选-20260629094530.csv")
    private String fileName;

    @Schema(description = "临时下载地址", example = "https://example.com/download/export.csv")
    private String downloadUrl;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

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
}
