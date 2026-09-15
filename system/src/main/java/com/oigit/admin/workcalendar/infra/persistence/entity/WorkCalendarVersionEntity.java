package com.oigit.admin.workcalendar.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oigit.admin.core.mybatis.BaseEntity;
import com.oigit.admin.workcalendar.enums.WorkCalendarVersionStatus;

import java.time.LocalDateTime;

@TableName("sys_work_calendar_version")
public class WorkCalendarVersionEntity extends BaseEntity {

    private Long id;

    @TableField("calendar_year_id")
    private Long calendarYearId;

    @TableField("calendar_year")
    private Integer calendarYear;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("status")
    private WorkCalendarVersionStatus status;

    @TableField("standard_periods_json")
    private String standardPeriodsJson;

    @TableField("content_hash")
    private String contentHash;

    @TableField("published_by")
    private Long publishedBy;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCalendarYearId() {
        return calendarYearId;
    }

    public void setCalendarYearId(Long calendarYearId) {
        this.calendarYearId = calendarYearId;
    }

    public Integer getCalendarYear() {
        return calendarYear;
    }

    public void setCalendarYear(Integer calendarYear) {
        this.calendarYear = calendarYear;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public WorkCalendarVersionStatus getStatus() {
        return status;
    }

    public void setStatus(WorkCalendarVersionStatus status) {
        this.status = status;
    }

    public String getStandardPeriodsJson() {
        return standardPeriodsJson;
    }

    public void setStandardPeriodsJson(String standardPeriodsJson) {
        this.standardPeriodsJson = standardPeriodsJson;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public Long getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(Long publishedBy) {
        this.publishedBy = publishedBy;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
