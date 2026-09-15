package com.oigit.admin.schedule.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Cron 表达式执行时间预演请求")
public class ScheduleJobCronPreviewReqDTO {

    @NotBlank(message = "Cron 表达式不能为空")
    @Schema(description = "Cron 表达式", example = "0 0/5 * * * ?")
    private String cronExpression;

    @Min(value = 1, message = "查询次数最小为 1")
    @Max(value = 10, message = "查询次数最大为 10")
    @Schema(description = "预演执行次数，默认 5，最大 10", example = "5")
    private Integer count = 5;

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
