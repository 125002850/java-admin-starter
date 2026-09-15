package com.oigit.admin.schedule.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.schedule.executor")
public class ScheduleExecutorProperties {

    private int poolSize = 4;

    private int awaitTerminationSeconds = 30;

    public int getPoolSize() { return poolSize; }
    public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    public int getAwaitTerminationSeconds() { return awaitTerminationSeconds; }
    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) { this.awaitTerminationSeconds = awaitTerminationSeconds; }
}
