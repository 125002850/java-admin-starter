package com.oigit.admin.export.domain.model;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.export.enums.ExportCenterErrorCode;
import com.oigit.admin.export.enums.ExportRecordStatus;

import java.time.LocalDateTime;

/**
 * 导出记录领域模型。数据库映射、MyBatis 注解和持久化状态不进入该对象。
 */
public class ExportRecord {

    private static final Long FALLBACK_OPERATOR_ID = 0L;

    private Long id;
    private String exportBizCode;
    private String exportBizName;
    private String fileName;
    private String fileType;
    private String contentType;
    private Long fileSize;
    private String objectKey;
    private String storageType;
    private ExportRecordStatus status;
    private LocalDateTime finishedTime;
    private LocalDateTime expireTime;
    private LocalDateTime deletedTime;
    private Integer deleteReason;
    private String failCode;
    private String failMessage;
    private String querySnapshotJson;
    private String querySnapshotSummary;
    private Integer downloadCount;
    private LocalDateTime lastDownloadTime;
    private Long lastDownloadBy;
    private Integer expireSeconds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Long deleted;
    private Integer version;

    public void initializeProcessing() {
        status = ExportRecordStatus.PROCESSING;
        if (deleted == null) {
            deleted = 0L;
        }
    }

    public void requireOwnedBy(Long operatorId) {
        Long ownerId = createBy == null ? FALLBACK_OPERATOR_ID : createBy;
        Long normalizedOperatorId = operatorId == null ? FALLBACK_OPERATOR_ID : operatorId;
        if (!ownerId.equals(normalizedOperatorId)) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_FORBIDDEN);
        }
    }

    public void requireDownloadable() {
        if (status != ExportRecordStatus.SUCCESS || objectKey == null || objectKey.isBlank()) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_DOWNLOADABLE);
        }
    }

    public void requireBatchDownloadable() {
        requireDownloadable();
        if (fileName == null
                || fileName.isBlank()
                || fileSize == null
                || fileSize < 0
                || "zip".equalsIgnoreCase(fileType)) {
            throw new BizException(ExportCenterErrorCode.EXPORT_RECORD_NOT_DOWNLOADABLE);
        }
    }

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

    public ExportRecordStatus getStatus() {
        return status;
    }

    public void setStatus(ExportRecordStatus status) {
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(Long deleted) {
        this.deleted = deleted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
