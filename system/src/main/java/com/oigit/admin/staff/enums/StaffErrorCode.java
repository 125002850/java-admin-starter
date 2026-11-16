package com.oigit.admin.staff.enums;

import com.oigit.admin.core.exception.ErrorCode;

public enum StaffErrorCode implements ErrorCode {

    STAFF_PASSWORD_IDENTITY_UNAVAILABLE(3004005, "无法识别当前登录账号，请重新登录后重试"),
    STAFF_PASSWORD_CONFIRMATION_MISMATCH(3004006, "两次输入的新密码不一致"),
    STAFF_PASSWORD_UNCHANGED(3004007, "新密码不能与当前密码相同"),
    STAFF_PASSWORD_CURRENT_INVALID(3004008, "当前密码不正确，请重新输入"),
    STAFF_PASSWORD_REJECTED(3004009, "密码修改未成功，请检查当前密码及新密码是否符合统一登录要求"),
    // 保留历史错误码，避免复用已发布编号；新改密请求返回下方的具体故障分类。
    STAFF_PASSWORD_SERVICE_UNAVAILABLE(3004010, "暂时无法确认密码修改结果，请稍后使用新密码登录确认"),
    STAFF_PASSWORD_CONFIG_MISSING(3004011, "密码修改服务配置缺失，请联系管理员"),
    STAFF_PASSWORD_CONFIG_INVALID(3004012, "密码修改服务配置无效，请联系管理员"),
    STAFF_PASSWORD_CALL_FAILED(3004013, "密码修改服务调用失败，请先使用新密码登录确认"),
    STAFF_PASSWORD_CALL_TIMEOUT(3004014, "密码修改服务调用超时，请先使用新密码登录确认"),
    STAFF_PASSWORD_HTTP_ERROR(3004015, "密码修改服务响应状态异常，请先使用新密码登录确认"),
    STAFF_PASSWORD_RESPONSE_INVALID(3004016, "密码修改服务响应格式错误，请先使用新密码登录确认"),
    STAFF_PASSWORD_RESPONSE_TOO_LARGE(3004017, "密码修改服务响应超过64 KiB限制，请先使用新密码登录确认"),
    STAFF_PASSWORD_RESPONSE_ENCODING_UNSUPPORTED(3004018, "密码修改服务响应编码不受支持，请先使用新密码登录确认");

    private final int code;
    private final String msg;

    StaffErrorCode(int code, String msg) {
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
