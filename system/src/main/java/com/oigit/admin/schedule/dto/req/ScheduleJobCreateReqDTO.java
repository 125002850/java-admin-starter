package com.oigit.admin.schedule.dto.req;

import com.oigit.admin.core.enums.EnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "定时任务创建请求")
public class ScheduleJobCreateReqDTO {

    @NotBlank
    @Schema(description = "任务名称", example = "示例维护任务")
    @jakarta.validation.constraints.Size(max = 128)
    private String jobName;

    @NotBlank
    @Schema(description = "任务编码", example = "maintenanceJob")
    @jakarta.validation.constraints.Size(max = 64)
    private String jobCode;

    @Schema(description = "默认cron表达式", example = "0 0/5 * * * ?")
    @jakarta.validation.constraints.Size(max = 128)
    private String defaultCron;

    @Schema(description = "cron表达式（当前生效）", example = "0 0/5 * * * ?")
    @jakarta.validation.constraints.Size(max = 128)
    private String cronExpression;

    @NotBlank
    @jakarta.validation.constraints.Size(max = 512)
    @Schema(description = "调用路由（接口URL或bean://beanName.methodName）", example = "bean://maintenanceJob.run")
    private String invokeRoute;

    @Schema(description = "启用状态", example = "enable")
    private EnableStatusEnum status;


    @Schema(description = "备注", example = "每5分钟执行维护任务")
    @jakarta.validation.constraints.Size(max = 512)
    private String remark;

    @Schema(description = "任务分组", example = "平台维护")
    @jakarta.validation.constraints.Size(max = 64)
    private String groupName;

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
    public EnableStatusEnum getStatus() { return status; }
    public void setStatus(EnableStatusEnum status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
