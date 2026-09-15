package com.oigit.admin.schedule;

import com.oigit.admin.schedule.domain.model.ScheduleJob;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobMapper;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobExecutionLogMapper;
import com.oigit.admin.schedule.infra.scheduling.ScheduleJobManager;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.scheduling.Trigger;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import static org.mockito.Mockito.*;

class ScheduleCommitTests {
    @Test void rollsBackWithoutRegisteringOrCancellingTasks() {
        var scheduler = mock(ThreadPoolTaskScheduler.class);
        var manager = new ScheduleJobManager(scheduler, mock(ScheduleJobMapper.class), mock(ScheduleJobExecutionLogMapper.class), List.of(), "Asia/Shanghai");
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
