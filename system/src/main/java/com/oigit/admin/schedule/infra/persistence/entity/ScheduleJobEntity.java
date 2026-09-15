package com.oigit.admin.schedule.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oigit.admin.core.enums.EnableStatusEnum;
import com.oigit.admin.core.mybatis.BaseEntity;

@TableName("sys_schedule_job")
public class ScheduleJobEntity extends BaseEntity {

    private Long id;

    @TableField("job_name")
    private String jobName;

    @TableField("job_code")
    private String jobCode;

    @TableField("default_cron")
    private String defaultCron;

    @TableField("cron_expression")
    private String cronExpression;

    @TableField("invoke_route")
    private String invokeRoute;

    @TableField("status")
    private EnableStatusEnum status;


    @TableField("remark")
    private String remark;

    @TableField("group_name")
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
}
