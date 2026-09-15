package com.oigit.admin.schedule.infra.provider;

import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

@Component
public class ReflectJobRunner implements JobRunner {

    private static final Logger log = LoggerFactory.getLogger(ReflectJobRunner.class);

    private final ApplicationContext applicationContext;
    private final java.util.Set<String> allowedRoutes;

    public ReflectJobRunner(ApplicationContext applicationContext,
            @org.springframework.beans.factory.annotation.Value("${platform.schedule.allowed-bean-routes:}") String allowedRoutes) {
        this.applicationContext = applicationContext;
        this.allowedRoutes = java.util.Arrays.stream(allowedRoutes.split(",")).map(String::trim).filter(value -> !value.isEmpty()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean supports(String invokeRoute) {
        return invokeRoute != null && invokeRoute.startsWith("bean://");
    }

    @Override
    public void execute(ScheduleJobEntity job) throws Exception {
        String route = Objects.requireNonNull(job.getInvokeRoute(), "invokeRoute must not be null");
        if (!allowedRoutes.contains(route)) throw new com.oigit.admin.core.exception.BizException(com.oigit.admin.schedule.enums.ScheduleErrorCode.JOB_EXECUTION_FAILED);
        String beanAndMethod = route.substring("bean://".length());

        int lastDot = beanAndMethod.lastIndexOf('.');
        if (lastDot <= 0 || lastDot >= beanAndMethod.length() - 1) {
            String msg = "Reflect job [" + job.getJobCode() + "] invalid route format: " + route;
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        String beanName = beanAndMethod.substring(0, lastDot);
        String methodName = beanAndMethod.substring(lastDot + 1);

        Object bean = applicationContext.getBean(beanName);
        Method method = bean.getClass().getMethod(methodName);
        method.invoke(bean);
        log.info("Reflect job [{}] executed successfully: {}.{}", job.getJobCode(), beanName, methodName);
    }
}
