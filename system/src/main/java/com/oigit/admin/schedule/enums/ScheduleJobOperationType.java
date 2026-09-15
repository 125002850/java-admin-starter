package com.oigit.admin.schedule.enums;

public enum ScheduleJobOperationType {

    CREATE("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    ENABLE("启用"),
    DISABLE("禁用");

    private final String desc;

    ScheduleJobOperationType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
