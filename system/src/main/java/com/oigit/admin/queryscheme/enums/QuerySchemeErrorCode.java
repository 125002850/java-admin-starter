package com.oigit.admin.queryscheme.enums;

import com.oigit.admin.core.exception.ErrorCode;

public enum QuerySchemeErrorCode implements ErrorCode {
    USER_REQUIRED(3012001, "当前用户信息缺失，无法管理查询方案"),
    SCENE_NOT_FOUND(3012002, "查询场景不存在"),
    CONTENT_INVALID(3012003, "查询方案条件已失效或格式不正确"),
    SCHEME_NOT_FOUND(3012004, "查询方案不存在"),
    NAME_DUPLICATED(3012005, "查询方案名称已存在"),
    LIMIT_EXCEEDED(3012006, "每个列表最多保存20个查询方案"),
    DATA_CHANGED(3012007, "查询方案已被其他操作更新，请刷新后重试");

    private final int code;
    private final String msg;

    QuerySchemeErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override public int getCode() { return code; }
    @Override public String getMsg() { return msg; }
}
