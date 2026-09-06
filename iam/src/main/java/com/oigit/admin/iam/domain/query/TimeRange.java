package com.oigit.admin.iam.domain.query;

import java.time.LocalDateTime;

public record TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
