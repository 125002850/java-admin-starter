package com.demo.core.export.model;

public class ExportStoreRequest {

    private String bizPath;
    private String objectKey;

    public String getBizPath() {
        return bizPath;
    }

    public void setBizPath(String bizPath) {
        this.bizPath = bizPath;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }
}
