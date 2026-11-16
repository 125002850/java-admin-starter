package com.oigit.admin.schedule.dto.rsp;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "定时任务最近一次执行的脱敏请求快照")
public record ScheduleJobRequestLogsRspDTO(
        @Schema(requiredMode = REQUIRED, description = "执行记录ID", example = "12")
        Long executionId,
        @Schema(requiredMode = REQUIRED, description = "是否仍有本次快照；新执行开始后旧快照被覆盖", example = "true")
        boolean available,
        @Schema(requiredMode = REQUIRED, description = "是否已结束报文采集", example = "true")
        boolean completed,
        @Schema(requiredMode = REQUIRED, description = "实际请求总数", example = "3")
        int totalRequests,
        @Schema(requiredMode = REQUIRED, description = "因快照容量上限未保留的请求数量", example = "0")
        int omittedRequests,
        @Schema(requiredMode = REQUIRED, description = "按请求顺序展示；每个正文最多64KiB，整份快照最多1MiB、200条")
        List<RequestRspDTO> requests) {

    @Schema(name = "ScheduleJobRequestLogRspDTO", description = "单次实际出站请求及返回报文，不保存认证头")
    public record RequestRspDTO(
            @Schema(requiredMode = REQUIRED, description = "本次执行内的请求序号", example = "1")
            int sequence,
            @Schema(requiredMode = REQUIRED, description = "请求开始时间")
            LocalDateTime startTime,
            @Schema(requiredMode = REQUIRED, description = "请求方法", example = "POST")
            String method,
            @Schema(requiredMode = REQUIRED, description = "脱敏请求地址", example = "https://example.com/query")
            String url,
            @Schema(requiredMode = REQUIRED, description = "脱敏请求参数")
            String requestParams,
            @Schema(requiredMode = REQUIRED, description = "脱敏请求报文，超限或不支持的格式明确标记省略")
            String requestBody,
            @Schema(description = "HTTP状态码；未收到响应时为空", example = "200")
            Integer statusCode,
            @Schema(requiredMode = REQUIRED, description = "脱敏返回报文，超限或不支持的格式明确标记省略")
            String responseBody,
            @Schema(requiredMode = REQUIRED, description = "请求耗时，毫秒", example = "120")
            long durationMillis,
            @Schema(description = "传输异常类型，不保存可能含敏感报文的原始异常消息")
            String errorType) { }
}
