package com.oigit.admin.core.export.model;

public class ExportMeta {

    private String fileName;
    private String fileType;
    private Integer expireSeconds;
    private String querySnapshotSummary;

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

    public Integer getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(Integer expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String getQuerySnapshotSummary() {
        return querySnapshotSummary;
    }

    public void setQuerySnapshotSummary(String querySnapshotSummary) {
        this.querySnapshotSummary = querySnapshotSummary;
    }
}
