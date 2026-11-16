package com.oigit.admin.schedule.infra.provider;

import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;
import com.oigit.admin.schedule.infra.scheduling.ScheduleRequestLogCollector;
import com.oigit.admin.schedule.enums.ScheduleErrorCode;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.http.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** 部署方明确配置目标 origin，禁止重定向和非幂等请求的自动重试。 */
@Component
public class HttpJobRunner implements JobRunner {
    private final OutboundHttpExecutor http;
    private final Set<String> allowedOrigins;
    private static final OutboundHttpClientOptions OPTIONS = OutboundHttpClientOptions.strict(
            Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofSeconds(35));

    public HttpJobRunner(@Value("${platform.schedule.allowed-http-origins:}") String origins, OutboundHttpExecutor http) {
        this.http = http;
        allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public boolean supports(String route) {
        return route != null && (route.startsWith("https://") || route.startsWith("http://"));
    }

    public void execute(ScheduleJobEntity job) throws Exception {
        URI uri = URI.create(job.getInvokeRoute());
        String origin = uri.getScheme() + "://" + uri.getRawAuthority();
        if (uri.getUserInfo() != null || uri.getHost() == null || !allowedOrigins.contains(origin)) {
            throw new BizException(ScheduleErrorCode.JOB_EXECUTION_FAILED);
        }
        var pending = ScheduleRequestLogCollector.start("POST", uri.toString(), null, new byte[0], "application/json");
        OutboundHttpResponse response;
        try {
            response = http.execute(OPTIONS, OutboundHttpRequests.jsonPost(uri.toString(), new byte[0]),
                    ScheduleRequestLogCollector.MAX_BODY_BYTES);
        } catch (Exception failure) {
            if (pending != null) pending.finish(null, null, null, failure);
            throw failure;
        }
        if (pending != null) pending.finish(response.statusCode(), response.body(), response.firstHeader("content-type"),
                null, response.bodyTooLarge() || response.unsupportedContentEncoding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BizException(ScheduleErrorCode.JOB_EXECUTION_FAILED);
        }
    }
}
