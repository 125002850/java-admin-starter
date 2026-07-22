package com.oigit.admin.staff.config;

import com.oigit.appcik.CIClient;
import com.oigit.appcik.CIConfig;
import com.oigit.appcik.DefaultStaffContextProvider;
import com.oigit.appcik.core.StaffContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "platform.sso-staff", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CIConfig.class)
public class StaffCiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StaffContextProvider staffContextProvider() {
        return new DefaultStaffContextProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public CIClient ciClient(CIConfig config, StaffContextProvider provider) {
        return CIClient.newBuilder(config, provider).build();
    }
}
