package com.oigit.admin.schedule.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "定时任务响应")
public class ScheduleJobRspDTO extends com.oigit.admin.core.web.AuditRspDTO {

    @Schema(description = "任务ID", example = "1")
    private Long id;

    @Schema(description = "任务名称", example = "示例维护任务")
    private String jobName;

    @Schema(description = "任务编码", example = "maintenanceJob")
    private String jobCode;

    @Schema(description = "默认cron表达式", example = "0 0/5 * * * ?")
    private String defaultCron;

    @Schema(description = "cron表达式（当前生效）", example = "0 0/5 * * * ?")
    private String cronExpression;

    @Schema(description = "调用路由", example = "bean://maintenanceJob.run")
    private String invokeRoute;

    @Schema(description = "启用状态：enable-启用，disable-禁用",
            allowableValues = {"enable", "disable"}, example = "enable")
    private String status;


    @Schema(description = "备注", example = "每5分钟执行维护任务")
    private String remark;

    @Schema(description = "任务分组", example = "平台维护")
    private String groupName;





    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }
    public String getDefaultCron() { return defaultCron; }
    public void setDefaultCron(String defaultCron) { this.defaultCron = defaultCron; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public String getInvokeRoute() { return invokeRoute; }
    public void setInvokeRoute(String invokeRoute) { this.invokeRoute = invokeRoute; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
