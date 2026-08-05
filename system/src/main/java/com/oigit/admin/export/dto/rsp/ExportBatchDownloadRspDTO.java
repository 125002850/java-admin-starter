package com.oigit.admin.export.dto.rsp;

import com.oigit.admin.export.enums.ExportRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量下载导出记录响应")
public class ExportBatchDownloadRspDTO {

    @Schema(description = "批量下载任务记录ID", example = "1001")
    private Long recordId;

    @Schema(description = "任务状态编码；显示名由前端字典 EXPORT_RECORD_STATUS 翻译", example = "1")
    private ExportRecordStatus status;

    @Schema(description = "打包文件名", example = "导出中心批量下载-20260629114000.zip")
    private String fileName;

    @Schema(description = "临时下载地址；异步任务提交时为空，任务成功后通过下载接口获取", example = "https://example.com/download/export-batch.zip")
    private String downloadUrl;

    @Schema(description = "内容类型，任务完成前为空", example = "application/zip")
    private String contentType;

    @Schema(description = "文件大小，单位字节，任务完成前为空", example = "2048")
    private Long fileSize;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public ExportRecordStatus getStatus() {
        return status;
    }

    public void setStatus(ExportRecordStatus status) {
        this.status = status;
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
