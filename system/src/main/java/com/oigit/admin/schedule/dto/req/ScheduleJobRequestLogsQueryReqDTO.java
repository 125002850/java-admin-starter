package com.oigit.admin.schedule.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "定时任务请求报文查询；仅最近一次执行有快照")
public class ScheduleJobRequestLogsQueryReqDTO extends ScheduleJobExecutionLogQueryReqDTO {
    @NotNull
    @Positive
    @Schema(description = "执行记录ID", example = "12")
    private Long executionId;

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }
}
