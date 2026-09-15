package com.oigit.admin.schedule.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "定时任务执行记录查询请求")
public class ScheduleJobExecutionLogQueryReqDTO {

    @NotNull
    @Schema(description = "任务ID", example = "1")
    private Long jobId;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
}
