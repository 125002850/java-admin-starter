package com.oigit.admin.file.infra.config;

import com.oigit.admin.file.domain.service.FileObjectKeyPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class FileDomainConfiguration {

    @Bean
    public FileObjectKeyPolicy fileObjectKeyPolicy(FileStorageProperties properties) {
        ZoneId zoneId = FileObjectKeyPolicy.resolveZoneId(properties.getZoneId());
        return new FileObjectKeyPolicy(zoneId, Clock.system(zoneId));
    }
}
