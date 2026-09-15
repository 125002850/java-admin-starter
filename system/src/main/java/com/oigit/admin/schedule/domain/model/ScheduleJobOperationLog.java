package com.oigit.admin.schedule.domain.model;


import java.time.LocalDateTime;

public class ScheduleJobOperationLog {

    private Long id;

    private Long jobId;

    private String jobCode;

    private String jobName;

    private String operationType;

    private String operationContent;

    private Long operatorId;

    private String operatorName;

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
