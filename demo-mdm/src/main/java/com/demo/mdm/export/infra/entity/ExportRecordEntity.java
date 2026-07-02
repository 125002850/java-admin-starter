package com.demo.mdm.export.infra.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.core.mybatis.BaseEntity;

import java.time.LocalDateTime;

@TableName("sys_export_record_global")
public class ExportRecordEntity extends BaseEntity {

    private Long id;

    @TableField("export_biz_code")
    private String exportBizCode;

    @TableField("export_biz_name")
    private String exportBizName;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("content_type")
    private String contentType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("object_key")
    private String objectKey;

    @TableField("storage_type")
    private String storageType;

    @TableField("status")
    private Integer status;

    @TableField("finished_time")
    private LocalDateTime finishedTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("deleted_time")
    private LocalDateTime deletedTime;

    @TableField("delete_reason")
    private Integer deleteReason;

    @TableField("fail_code")
    private String failCode;

    @TableField("fail_message")
    private String failMessage;

    @TableField("query_snapshot_json")
    private String querySnapshotJson;

    @TableField("query_snapshot_summary")
    private String querySnapshotSummary;

    @TableField("download_count")
    private Integer downloadCount;

    @TableField("last_download_time")
    private LocalDateTime lastDownloadTime;

    @TableField("last_download_by")
    private Long lastDownloadBy;

    @TableField("expire_seconds")
    private Integer expireSeconds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public LocalDateTime getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(LocalDateTime deletedTime) {
        this.deletedTime = deletedTime;
    }

    public Integer getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(Integer deleteReason) {
        this.deleteReason = deleteReason;
    }

    public String getFailCode() {
        return failCode;
    }

    public void setFailCode(String failCode) {
        this.failCode = failCode;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    public String getQuerySnapshotJson() {
        return querySnapshotJson;
    }

    public void setQuerySnapshotJson(String querySnapshotJson) {
        this.querySnapshotJson = querySnapshotJson;
    }

    public String getQuerySnapshotSummary() {
        return querySnapshotSummary;
    }

    public void setQuerySnapshotSummary(String querySnapshotSummary) {
        this.querySnapshotSummary = querySnapshotSummary;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public LocalDateTime getLastDownloadTime() {
        return lastDownloadTime;
    }

    public void setLastDownloadTime(LocalDateTime lastDownloadTime) {
        this.lastDownloadTime = lastDownloadTime;
    }

    public Long getLastDownloadBy() {
        return lastDownloadBy;
    }

    public void setLastDownloadBy(Long lastDownloadBy) {
        this.lastDownloadBy = lastDownloadBy;
    }

    public Integer getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(Integer expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
