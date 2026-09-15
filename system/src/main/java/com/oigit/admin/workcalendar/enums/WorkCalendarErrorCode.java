package com.oigit.admin.workcalendar.enums;

import com.oigit.admin.core.exception.ErrorCode;

public enum WorkCalendarErrorCode implements ErrorCode {

    WORK_CALENDAR_YEAR_INVALID(3006001, "工作日历年份超出支持范围"),
    WORK_CALENDAR_DRAFT_NOT_FOUND(3006002, "工作日历草稿不存在"),
    WORK_CALENDAR_VERSION_CONFLICT(3006003, "工作日历草稿已被其他操作更新"),
    WORK_CALENDAR_PERIOD_INVALID(3006004, "工作时段配置不合法"),
    WORK_CALENDAR_PERIOD_OVERLAP(3006005, "工作时段存在重叠"),
    WORK_CALENDAR_DATE_OVERRIDE_INVALID(3006006, "特殊日期配置不合法"),
    WORK_CALENDAR_IMPORT_INVALID(3006007, "工作日历导入文件不合法"),
    WORK_CALENDAR_PUBLISH_VALIDATION_FAILED(3006008, "工作日历全年校验未通过"),
    WORK_CALENDAR_BASELINE_MISSING(3006009, "工作日历缺少可用的已发布基线"),
    WORK_CALENDAR_VERSION_IMMUTABLE(3006010, "已发布工作日历版本不可修改"),
    WORK_CALENDAR_PUBLISHED_YEAR_MISSING(3006011, "目标年度工作日历尚未发布"),
    WORK_CALENDAR_QUERY_RANGE_INVALID(3006012, "工作日历查询范围非法");

    private final int code;
    private final String msg;

    WorkCalendarErrorCode(int code, String msg) {
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
