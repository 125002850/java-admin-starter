package com.oigit.admin.schedule.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleRequestSnapshot(boolean completed, int totalRequests, List<RequestLog> requests) {
    public static ScheduleRequestSnapshot empty() {
        return new ScheduleRequestSnapshot(false, 0, List.of());
    }

    public record RequestLog(int sequence, LocalDateTime startTime, String method, String url,
            String requestParams, String requestBody, Integer statusCode, String responseBody,
            long durationMillis, String errorType) { }
}
