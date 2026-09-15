package com.oigit.admin.schedule.infra.provider;

import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;

public interface JobRunner {

    boolean supports(String invokeRoute);

    void execute(ScheduleJobEntity job) throws Exception;
}
