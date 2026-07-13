package com.example.admin.core.operator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "platform.operator.cache-user")
public class CacheUserProperties {

    private long updateWindowSeconds = 600L;

    public long getUpdateWindowSeconds() {
        return updateWindowSeconds;
    }

    public void setUpdateWindowSeconds(long updateWindowSeconds) {
        this.updateWindowSeconds = updateWindowSeconds;
    }
}
