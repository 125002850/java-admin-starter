package com.oigit.admin.export.dto.rsp;

import com.oigit.admin.core.web.AuditRspDTO;
import com.oigit.admin.export.enums.ExportRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "导出记录响应")
public class ExportRecordRspDTO extends AuditRspDTO {

    @Schema(description = "记录ID", example = "1")
    private Long recordId;

    @Schema(description = "导出场景编码", example = "mdm.global.dict.type.list")
    private String exportBizCode;

    @Schema(description = "导出场景名称", example = "全局字典类型列表导出")
    private String exportBizName;

    @Schema(description = "文件名", example = "全局字典类型-筛选-20260629094530.csv")
    private String fileName;

    @Schema(description = "文件类型", example = "csv")
    private String fileType;

    @Schema(description = "状态编码；显示名由前端字典 EXPORT_RECORD_STATUS 翻译", example = "2")
    private ExportRecordStatus status;

    @Schema(description = "内容类型", example = "text/csv;charset=UTF-8")
    private String contentType;

    @Schema(description = "文件大小", example = "1024")
    private Long fileSize;

    @Schema(description = "下载链接获取次数", example = "1")
    private Integer downloadCount;

    @Schema(description = "查询快照摘要", example = "keyword=status")
    private String querySnapshotSummary;

    @Schema(description = "完成时间")
    private LocalDateTime finishedTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getExportBizCode() {
        return exportBizCode;
    }

    public void setExportBizCode(String exportBizCode) {
        this.exportBizCode = exportBizCode;
    }

    public String getExportBizName() {
        return exportBizName;
    }

    public void setExportBizName(String exportBizName) {
        this.exportBizName = exportBizName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public ExportRecordStatus getStatus() {
        return status;
    }

    public void setStatus(ExportRecordStatus status) {
        this.status = status;
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

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public String getQuerySnapshotSummary() {
        return querySnapshotSummary;
    }

    public void setQuerySnapshotSummary(String querySnapshotSummary) {
        this.querySnapshotSummary = querySnapshotSummary;
    }

    public LocalDateTime getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(LocalDateTime finishedTime) {
        this.finishedTime = finishedTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

}
