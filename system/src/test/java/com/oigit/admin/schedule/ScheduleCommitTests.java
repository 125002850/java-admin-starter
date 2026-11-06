package com.oigit.admin.schedule;

import com.oigit.admin.schedule.domain.model.ScheduleJob;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobMapper;
import com.oigit.admin.schedule.infra.persistence.service.ScheduleJobLogService;
import com.oigit.admin.schedule.infra.scheduling.ScheduleJobManager;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.scheduling.Trigger;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import static org.mockito.Mockito.*;

class ScheduleCommitTests {
    @Test void commitsRunningBeforeExecutionAndFinishesSameLogAfterFailure() throws Exception {
        var scheduler = mock(ThreadPoolTaskScheduler.class);
        var logs = mock(ScheduleJobLogService.class);
        var runner = mock(com.oigit.admin.schedule.infra.provider.JobRunner.class);
        when(runner.supports(any())).thenReturn(true);
        when(logs.startExecution(any())).thenReturn(7L);
        doAnswer(invocation -> { invocation.<Runnable>getArgument(0).run(); return null; })
                .when(scheduler).execute(any(Runnable.class));
        doThrow(new IllegalStateException("业务失败")).when(runner).execute(any());
        var manager = new ScheduleJobManager(scheduler, mock(ScheduleJobMapper.class), logs, List.of(runner), "Asia/Shanghai");
        manager.triggerJob(new ScheduleJob());
        var order = inOrder(logs, runner);
        order.verify(logs).startExecution(any());
        order.verify(runner).execute(any());
        order.verify(logs).finishExecution(7L, "业务失败");
        reset(logs, runner);
        when(logs.startExecution(any())).thenThrow(new IllegalStateException("日志写入失败"));
        manager.triggerJob(new ScheduleJob());
        verifyNoInteractions(runner);
        verify(logs, never()).finishExecution(any(), any());
    }

    @Test void rollsBackWithoutRegisteringOrCancellingTasks() {
        var scheduler = mock(ThreadPoolTaskScheduler.class);
        var manager = new ScheduleJobManager(scheduler, mock(ScheduleJobMapper.class), mock(ScheduleJobLogService.class), List.of(), "Asia/Shanghai");
        var job = new ScheduleJob(); job.setJobCode("commit-test"); job.setDefaultCron("0 0 9 * * *");
        TransactionSynchronizationManager.initSynchronization();
        try {
            manager.scheduleJob(job);
            verifyNoInteractions(scheduler);
            // Clearing a rolled-back synchronization never calls afterCommit.
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
        verifyNoInteractions(scheduler);
        TransactionSynchronizationManager.initSynchronization();
        try {
            doReturn(mock(ScheduledFuture.class)).when(scheduler).schedule(any(Runnable.class), any(Trigger.class));
            manager.scheduleJob(job);
            verify(scheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(scheduler).schedule(any(Runnable.class), any(Trigger.class));
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
