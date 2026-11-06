package com.oigit.admin.schedule.infra.scheduling;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oigit.admin.core.enums.EnableStatusEnum;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;
import com.oigit.admin.schedule.infra.persistence.service.ScheduleJobLogService;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobMapper;
import com.oigit.admin.schedule.infra.provider.JobRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class ScheduleJobManager implements com.oigit.admin.schedule.domain.gateway.JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScheduleJobManager.class);
    private final TimeZone zone;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final ScheduleJobMapper scheduleJobMapper;
    private final ScheduleJobLogService jobLogService;
    private final List<JobRunner> runners;
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public ScheduleJobManager(
            ThreadPoolTaskScheduler taskScheduler,
            ScheduleJobMapper scheduleJobMapper,
            ScheduleJobLogService jobLogService,
            List<JobRunner> runners,
            @org.springframework.beans.factory.annotation.Value("${platform.schedule.time-zone:Asia/Shanghai}") String timeZone) {
        this.taskScheduler = taskScheduler;
        this.scheduleJobMapper = scheduleJobMapper;
        this.jobLogService = jobLogService;
        this.runners = runners;
        this.zone = TimeZone.getTimeZone(java.time.ZoneId.of(timeZone));
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener(ApplicationReadyEvent.class)
    void init() {
        List<ScheduleJobEntity> enabledJobs = scheduleJobMapper.selectList(
                new LambdaQueryWrapper<ScheduleJobEntity>()
                        .eq(ScheduleJobEntity::getStatus, EnableStatusEnum.ENABLE)
                        .and(w -> w.isNotNull(ScheduleJobEntity::getCronExpression)
                                .or()
                                .isNotNull(ScheduleJobEntity::getDefaultCron))
        );
        log.info("ScheduleJobManager loading {} enabled jobs on startup", enabledJobs.size());
        enabledJobs.forEach(this::scheduleEntity);
    }

    private void scheduleEntity(ScheduleJobEntity job) {
        String cron = resolveCron(job);
        if (cron == null) {
            log.warn("ScheduleJobManager skipping job [{}]: no cron expression configured", job.getJobCode());
            return;
        }

        cancelEntity(job.getJobCode());

        try {
            CronTrigger trigger = new CronTrigger(cron, zone);
            Runnable task = () -> executeJob(job);
            ScheduledFuture<?> future = taskScheduler.schedule(task, trigger);
            futures.put(job.getJobCode(), future);
            log.info("ScheduleJobManager scheduled job [{}] with cron [{}]", job.getJobCode(), cron);
        } catch (IllegalArgumentException e) {
            log.error("ScheduleJobManager failed to schedule job [{}]: invalid cron [{}]", job.getJobCode(), cron, e);
        }
    }

    private void cancelEntity(String jobCode) {
        ScheduledFuture<?> future = futures.remove(jobCode);
        if (future != null) {
            future.cancel(false);
            log.info("ScheduleJobManager cancelled job [{}]", jobCode);
        }
    }

    private void triggerEntity(ScheduleJobEntity job) {
        log.info("ScheduleJobManager triggering job [{}] immediately", job.getJobCode());
        taskScheduler.execute(() -> executeJob(job));
    }

    private void executeJob(ScheduleJobEntity job) {
        Long executionId;
        try {
            executionId = jobLogService.startExecution(job);
        } catch (RuntimeException exception) {
            log.error("Failed to start execution log for [{}]", job.getJobCode(), exception);
            return;
        }
        String failure = null;
        try {
            JobRunner runner = runners.stream()
                    .filter(r -> r.supports(job.getInvokeRoute())).findFirst().orElse(null);
            if (runner == null) {
                failure = "不支持的调用路由: " + job.getInvokeRoute();
            } else {
                runner.execute(job);
            }
        } catch (Exception exception) {
            failure = org.springframework.util.StringUtils.hasText(exception.getMessage())
                    ? exception.getMessage() : exception.getClass().getSimpleName();
            log.error("Job [{}] execution failed", job.getJobCode(), exception);
        }
        if (failure != null && failure.length() > 2000) {
            failure = failure.substring(0, 2000);
        }
        try {
            jobLogService.finishExecution(executionId, failure);
        } catch (RuntimeException exception) {
            // 完成日志落库失败不重新执行已完成的业务。
            log.error("Failed to finish execution log [{}] for [{}]", executionId, job.getJobCode(), exception);
        }
    }

    private String resolveCron(ScheduleJobEntity job) {
        if (StringUtils.hasText(job.getCronExpression())) {
            return job.getCronExpression();
        }
        if (StringUtils.hasText(job.getDefaultCron())) {
            return job.getDefaultCron();
        }
        return null;
    }

    public void scheduleJob(com.oigit.admin.schedule.domain.model.ScheduleJob job) {
        var entity = com.oigit.admin.schedule.infra.persistence.repository.ScheduleModelMapper.toEntity(job);
        afterCommit(() -> scheduleEntity(entity));
    }
    public void triggerJob(com.oigit.admin.schedule.domain.model.ScheduleJob job) {
        var entity = com.oigit.admin.schedule.infra.persistence.repository.ScheduleModelMapper.toEntity(job);
        afterCommit(() -> triggerEntity(entity));
    }
    public void cancelJob(String code) { afterCommit(() -> cancelEntity(code)); }
    private void afterCommit(Runnable action) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { action.run(); }
                });
        } else action.run();
    }
}
