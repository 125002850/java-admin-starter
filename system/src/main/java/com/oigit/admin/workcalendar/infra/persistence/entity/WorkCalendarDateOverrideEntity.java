package com.oigit.admin.workcalendar.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oigit.admin.core.mybatis.BaseEntity;
import com.oigit.admin.workcalendar.enums.WorkCalendarDateOverrideType;

import java.time.LocalDate;

@TableName("sys_work_calendar_date_override")
public class WorkCalendarDateOverrideEntity extends BaseEntity {

    private Long id;

    @TableField("calendar_version_id")
    private Long calendarVersionId;

    @TableField("calendar_date")
    private LocalDate calendarDate;

    @TableField("override_type")
    private WorkCalendarDateOverrideType overrideType;

    @TableField("date_name")
    private String dateName;

    @TableField("custom_periods_json")
    private String customPeriodsJson;

    @TableField("source_note")
    private String sourceNote;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCalendarVersionId() {
        return calendarVersionId;
    }

    public void setCalendarVersionId(Long calendarVersionId) {
        this.calendarVersionId = calendarVersionId;
    }

    public LocalDate getCalendarDate() {
        return calendarDate;
    }

    public void setCalendarDate(LocalDate calendarDate) {
        this.calendarDate = calendarDate;
    }

    public WorkCalendarDateOverrideType getOverrideType() {
        return overrideType;
    }

    public void setOverrideType(WorkCalendarDateOverrideType overrideType) {
        this.overrideType = overrideType;
    }

    public String getDateName() {
        return dateName;
    }

    public void setDateName(String dateName) {
        this.dateName = dateName;
    }

    public String getCustomPeriodsJson() {
        return customPeriodsJson;
    }

    public void setCustomPeriodsJson(String customPeriodsJson) {
        this.customPeriodsJson = customPeriodsJson;
    }

    public String getSourceNote() {
        return sourceNote;
    }

    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }
}
