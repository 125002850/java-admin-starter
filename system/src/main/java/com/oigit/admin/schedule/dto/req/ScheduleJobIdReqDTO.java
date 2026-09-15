package com.oigit.admin.schedule.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "定时任务ID请求")
public class ScheduleJobIdReqDTO {

    @NotNull
    @Schema(description = "任务ID", example = "1")
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
