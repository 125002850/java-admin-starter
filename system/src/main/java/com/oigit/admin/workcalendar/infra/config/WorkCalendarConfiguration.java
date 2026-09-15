package com.oigit.admin.workcalendar.infra.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.oigit.admin.workcalendar.domain.service.WorkCalendarRuleService;
@Configuration
public class WorkCalendarConfiguration {
    @Bean WorkCalendarRuleService workCalendarRuleService() { return new WorkCalendarRuleService(); }
}
