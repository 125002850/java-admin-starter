package com.example.admin.file.enums;

import com.example.admin.core.exception.ErrorCode;

public enum FileErrorCode implements ErrorCode {

    EMPTY_FILE(3002001, "上传文件不能为空"),
    INVALID_BIZ_PATH(3002002, "业务路径格式非法"),
    INVALID_OBJECT_KEY(3002003, "对象键格式非法"),
    FILE_NOT_FOUND(3002004, "文件不存在"),
    FILE_UPLOAD_FAILED(3002005, "文件上传失败"),
    FILE_DELETE_FAILED(3002006, "文件删除失败"),
    TEMP_URL_GENERATE_FAILED(3002007, "临时访问地址生成失败"),
    DIRECT_UPLOAD_NOT_SUPPORTED(3002008, "当前存储类型不支持直传凭证"),
    INVALID_STORAGE_CONFIG(3002009, "文件存储配置非法"),
    DIRECT_UPLOAD_CREDENTIAL_GENERATE_FAILED(3002010, "直传凭证生成失败"),
    FILE_DOWNLOAD_FAILED(3002011, "文件下载失败");

    private final int code;
    private final String msg;

    FileErrorCode(int code, String msg) {
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
