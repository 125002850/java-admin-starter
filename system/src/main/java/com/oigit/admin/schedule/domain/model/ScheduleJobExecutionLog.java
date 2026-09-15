package com.oigit.admin.schedule.domain.model;


import java.time.LocalDateTime;

public class ScheduleJobExecutionLog {

    private Long id;

    private Long jobId;

    private String jobCode;

    private String jobName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private String errorMessage;

    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
