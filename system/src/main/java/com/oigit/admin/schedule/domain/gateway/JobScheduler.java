package com.oigit.admin.schedule.domain.gateway;
import com.oigit.admin.schedule.domain.model.ScheduleJob;
public interface JobScheduler {
 void scheduleJob(ScheduleJob job);
 void cancelJob(String jobCode);
 void triggerJob(ScheduleJob job);
}
