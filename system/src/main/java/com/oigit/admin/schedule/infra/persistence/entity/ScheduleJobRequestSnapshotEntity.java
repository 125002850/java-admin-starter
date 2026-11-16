package com.oigit.admin.schedule.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 可覆盖的排查快照，按任务物理替换，不保留历史版本。 */
@TableName("sys_schedule_job_request_snapshot")
public class ScheduleJobRequestSnapshotEntity {
    @TableId(type = IdType.INPUT)
    private Long jobId;
    private Long executionId;
    private LocalDateTime startTime;
    private String snapshotJson;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
}
