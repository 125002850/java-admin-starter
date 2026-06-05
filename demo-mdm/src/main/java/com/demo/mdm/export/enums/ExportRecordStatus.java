package com.demo.mdm.export.enums;

public enum ExportRecordStatus {

    PROCESSING(1),
    SUCCESS(2),
    FAILED(3),
    EXPIRED(4),
    DELETED(5);

    private final int code;

    ExportRecordStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
