package com.demo.mdm.export.enums;

import com.demo.core.exception.ErrorCode;

public enum ExportCenterErrorCode implements ErrorCode {

    EXPORT_RECORD_NOT_FOUND(3001101, "导出记录不存在"),
    EXPORT_RECORD_STATUS_INVALID(3001102, "导出记录状态非法"),
    EXPORT_RECORD_FORBIDDEN(3001103, "无权操作该导出记录"),
    EXPORT_RECORD_NOT_DOWNLOADABLE(3001104, "当前导出记录不可下载");

    private final int code;
    private final String msg;

    ExportCenterErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
