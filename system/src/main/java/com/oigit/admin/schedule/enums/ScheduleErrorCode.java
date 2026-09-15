package com.oigit.admin.schedule.enums;

import com.oigit.admin.core.exception.ErrorCode;

public enum ScheduleErrorCode implements ErrorCode {

    JOB_NOT_FOUND(3005001, "定时任务不存在"),
    JOB_CODE_DUPLICATE(3005002, "任务编码已存在"),
    JOB_CRON_INVALID(3005003, "cron表达式无效"),
    JOB_RUNNER_NOT_SUPPORTED(3005004, "不支持的调用路由"),
    JOB_EXECUTION_FAILED(3005005, "定时任务执行失败"),
    JOB_DISABLED_TRIGGER_DENIED(3005006, "禁用状态的任务无法触发立即执行"),
    JOB_ENABLED_DELETE_DENIED(3005007, "请先禁用任务后再删除"),
    JOB_VERSION_CONFLICT(3005008, "任务已被其他操作修改，请刷新后重试");

    private final int code;
    private final String msg;

    ScheduleErrorCode(int code, String msg) {
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
