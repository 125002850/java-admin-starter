package com.oigit.admin.schedule.enums;

/** 执行记录沿用字符串契约，与任务的启停状态独立。 */
public enum ScheduleJobExecutionStatus {
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed");

    private final String code;

    ScheduleJobExecutionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
