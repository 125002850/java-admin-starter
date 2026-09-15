package com.oigit.admin.workcalendar.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oigit.admin.core.mybatis.BaseEntity;

@TableName("sys_work_calendar_year")
public class WorkCalendarYearEntity extends BaseEntity {

    private Long id;

    @TableField("calendar_year")
    private Integer calendarYear;

    @TableField("active_version_id")
    private Long activeVersionId;

    @TableField("draft_version_id")
    private Long draftVersionId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCalendarYear() {
        return calendarYear;
    }

    public void setCalendarYear(Integer calendarYear) {
        this.calendarYear = calendarYear;
    }

    public Long getActiveVersionId() {
        return activeVersionId;
    }

    public void setActiveVersionId(Long activeVersionId) {
        this.activeVersionId = activeVersionId;
    }

    public Long getDraftVersionId() {
        return draftVersionId;
    }

    public void setDraftVersionId(Long draftVersionId) {
        this.draftVersionId = draftVersionId;
    }
}
