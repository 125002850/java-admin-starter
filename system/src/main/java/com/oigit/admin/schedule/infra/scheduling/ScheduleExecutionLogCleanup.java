package com.oigit.admin.schedule.infra.scheduling;

import com.oigit.admin.schedule.infra.persistence.service.ScheduleJobLogService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 清理必须显式启用，避免升级模板时自动删除已有历史。 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "platform.schedule.execution-log-cleanup-enabled", havingValue = "true")
public class ScheduleExecutionLogCleanup {
    private final ScheduleJobLogService logs;
    private final int retentionDays;

    public ScheduleExecutionLogCleanup(ScheduleJobLogService logs,
            @Value("${platform.schedule.execution-log-retention-days:7}") int retentionDays) {
        if (retentionDays < 1) throw new IllegalArgumentException("retention-days must be positive");
        this.logs = logs;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${platform.schedule.execution-log-cleanup-cron:0 0 3 * * ?}",
            zone = "${platform.schedule.time-zone:Asia/Shanghai}")
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        while (logs.deleteExpiredExecutions(threshold) > 0) {
            // 每批独立事务；不触碰执行中记录。
        }
    }
}
