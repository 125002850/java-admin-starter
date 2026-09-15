package com.oigit.admin.schedule.domain.model;

import com.oigit.admin.core.enums.EnableStatusEnum;
import java.time.LocalDateTime;

public class ScheduleJob  {

    private Long id;

    private String jobName;

    private String jobCode;

    private String defaultCron;

    private String cronExpression;

    private String invokeRoute;

    private EnableStatusEnum status;


    private String remark;

    private String groupName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobCode() {
        return jobCode;
    }

    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }

    public String getDefaultCron() {
        return defaultCron;
    }

    public void setDefaultCron(String defaultCron) {
        this.defaultCron = defaultCron;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getInvokeRoute() {
        return invokeRoute;
    }

    public void setInvokeRoute(String invokeRoute) {
        this.invokeRoute = invokeRoute;
    }

    public EnableStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnableStatusEnum status) {
        this.status = status;
    }



    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private Long deleted;

    private Integer version;

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(Long deleted) {
        this.deleted = deleted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
