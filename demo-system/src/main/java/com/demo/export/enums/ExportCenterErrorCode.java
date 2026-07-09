package com.demo.export.enums;

import com.demo.core.exception.ErrorCode;

public enum ExportCenterErrorCode implements ErrorCode {

    EXPORT_RECORD_NOT_FOUND(3001101, "导出记录不存在"),
    EXPORT_RECORD_STATUS_INVALID(3001102, "导出记录状态非法"),
    EXPORT_RECORD_FORBIDDEN(3001103, "无权操作该导出记录"),
    EXPORT_RECORD_NOT_DOWNLOADABLE(3001104, "当前导出记录不可下载"),
    EXPORT_SCENE_NOT_FOUND(3001105, "导出场景不存在"),
    EXPORT_FILE_TYPE_NOT_SUPPORTED(3001106, "导出文件类型暂不支持"),
    EXPORT_QUERY_INVALID(3001107, "导出参数不合法"),
    EXPORT_EXECUTION_FAILED(3001108, "导出执行失败"),
    EXPORT_QUERY_ROW_LIMIT_EXCEEDED(3001109, "导出结果行数超限"),
    EXPORT_BATCH_RECORD_EMPTY(3001110, "请选择需要下载的导出记录"),
    EXPORT_BATCH_RECORD_LIMIT_EXCEEDED(3001111, "批量下载记录数超限"),
    EXPORT_BATCH_FILE_SIZE_LIMIT_EXCEEDED(3001112, "批量下载文件总大小超限");

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
