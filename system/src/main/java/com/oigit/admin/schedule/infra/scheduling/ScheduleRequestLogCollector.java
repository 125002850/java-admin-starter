package com.oigit.admin.schedule.infra.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.oigit.admin.core.logging.HttpLogSanitizer;
import com.oigit.admin.core.trace.TraceIdFilter;
import com.oigit.admin.schedule.domain.model.ScheduleRequestSnapshot;
import com.oigit.admin.schedule.domain.model.ScheduleRequestSnapshot.RequestLog;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** 仅采集当前调度执行线程；不改变外部请求、响应或异常。 */
@Component
public class ScheduleRequestLogCollector {
    public static final int MAX_BODY_BYTES = 64 * 1024;
    public static final int MAX_SNAPSHOT_BYTES = 1024 * 1024;
    public static final int MAX_REQUESTS = 200;
    private static final Logger log = LoggerFactory.getLogger(ScheduleRequestLogCollector.class);
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    private final ObjectMapper objectMapper;
    private final HttpLogSanitizer sanitizer;

    public ScheduleRequestLogCollector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.sanitizer = new HttpLogSanitizer(objectMapper.copy()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS),
                Set.of("appKey", "apiKey", "sign", "signature", "email", "address"));
    }

    public Scope open(String jobCode, Long executionId) {
        Scope scope = new Scope(jobCode, executionId);
        CURRENT.set(scope);
        if (MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY) == null) {
            MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, UUID.randomUUID().toString().replace("-", ""));
        }
        MDC.put("scheduleJobCode", jobCode);
        MDC.put("scheduleExecutionId", String.valueOf(executionId));
        return scope;
    }

    public static PendingRequest start(String method, String url, Object params, Object body, String contentType) {
        Scope scope = CURRENT.get();
        return scope == null ? null : scope.start(method, url, params, body, contentType);
    }

    private String body(Object value, String contentType) {
        if (value == null) return "-";
        try {
            byte[] bytes = value instanceof byte[] raw ? raw : objectMapper.writeValueAsBytes(value);
            return sanitizer.sanitizeBody(bytes, contentType, "UTF-8", true,
                    bytes.length > MAX_BODY_BYTES, MAX_BODY_BYTES);
        } catch (RuntimeException | JsonProcessingException exception) {
            return "<omitted: unreadable body>";
        }
    }

    private String url(String value) {
        try {
            URI uri = URI.create(value);
            // 不记录 user-info 或 fragment，查询参数复用现有字段脱敏。
            String query = uri.getRawQuery();
            return sanitizer.singleLine(uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() < 0 ? "" : ":" + uri.getPort()) + uri.getRawPath()
                    + (query == null ? "" : "?" + sanitizer.sanitizeQuery(query)));
        } catch (RuntimeException exception) {
            return "<omitted: invalid URL>";
        }
    }

    public final class Scope implements AutoCloseable {
        private final Scope previous = CURRENT.get();
        private final Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        private final String jobCode;
        private final Long executionId;
        private final List<RequestLog> requests = new ArrayList<>();
        private int totalRequests;
        private int snapshotBytes = 128;

        private Scope(String jobCode, Long executionId) {
            this.jobCode = jobCode;
            this.executionId = executionId;
        }

        private PendingRequest start(String method, String requestUrl, Object params, Object requestBody,
                String contentType) {
            return new PendingRequest(this, ++totalRequests, LocalDateTime.now(), System.nanoTime(),
                    sanitizer.singleLine(method), url(requestUrl), body(params, "application/json"),
                    body(requestBody, contentType));
        }

        private void add(RequestLog entry) {
            try {
                String json = objectMapper.writeValueAsString(entry);
                int size = json.getBytes(StandardCharsets.UTF_8).length + 1;
                if (requests.size() < MAX_REQUESTS && snapshotBytes + size <= MAX_SNAPSHOT_BYTES) {
                    requests.add(entry);
                    snapshotBytes += size;
                }
            } catch (RuntimeException | JsonProcessingException exception) {
                log.warn("Schedule request diagnostic unavailable: executionId={}, sequence={}",
                        executionId, entry.sequence());
            }
        }

        private String responseBody(byte[] response, String contentType) {
            return body(response, contentType);
        }

        public ScheduleRequestSnapshot snapshot() {
            return new ScheduleRequestSnapshot(true, totalRequests, List.copyOf(requests));
        }

        @Override
        public void close() {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
            if (previousMdc == null) MDC.clear();
            else MDC.setContextMap(previousMdc);
        }
    }

    public static final class PendingRequest {
        private final Scope scope;
        private final int sequence;
        private final LocalDateTime startTime;
        private final long startedAt;
        private final String method;
        private final String url;
        private final String params;
        private final String requestBody;

        private PendingRequest(Scope scope, int sequence, LocalDateTime startTime, long startedAt,
                String method, String url, String params, String requestBody) {
            this.scope = scope;
            this.sequence = sequence;
            this.startTime = startTime;
            this.startedAt = startedAt;
            this.method = method;
            this.url = url;
            this.params = params;
            this.requestBody = requestBody;
        }

        public void finish(Integer status, byte[] response, String contentType, Throwable failure) {
            finish(status, response, contentType, failure, false);
        }

        public void finish(Integer status, byte[] response, String contentType, Throwable failure, boolean omitted) {
            scope.add(new RequestLog(sequence, startTime, method, url, params, requestBody, status,
                    omitted ? "<omitted: response exceeds capture limit or unsupported encoding>"
                            : scope.responseBody(response, contentType),
                    Math.max(0, (System.nanoTime() - startedAt) / 1_000_000),
                    failure == null ? null : failure.getClass().getSimpleName()));
        }
    }
}
