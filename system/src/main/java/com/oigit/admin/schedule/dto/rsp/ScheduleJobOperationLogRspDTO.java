package com.oigit.admin.schedule.dto.rsp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "定时任务操作记录响应")
public class ScheduleJobOperationLogRspDTO {

    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "任务ID", example = "1")
    private Long jobId;

    @Schema(description = "任务编码", example = "maintenanceJob")
    private String jobCode;

    @Schema(description = "任务名称", example = "示例维护任务")
    private String jobName;

    @Schema(description = "操作类型", example = "CREATE")
    private String operationType;

    @Schema(description = "操作内容（JSON）", example = "{\"jobName\":\"示例维护\"}")
    private String operationContent;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人名称", example = "admin")
    private String operatorName;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getOperationContent() { return operationContent; }
    public void setOperationContent(String operationContent) { this.operationContent = operationContent; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
