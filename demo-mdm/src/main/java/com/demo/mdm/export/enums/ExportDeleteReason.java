package com.demo.mdm.export.enums;

public enum ExportDeleteReason {

    MANUAL(1),
    EXPIRED_CLEANUP(2);

    private final int code;

    ExportDeleteReason(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
