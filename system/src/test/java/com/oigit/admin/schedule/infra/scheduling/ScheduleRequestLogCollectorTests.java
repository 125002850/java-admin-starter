package com.oigit.admin.schedule.infra.scheduling;
import com.oigit.admin.schedule.domain.model.ScheduleRequestSnapshot;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class ScheduleRequestLogCollectorTests {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final ScheduleRequestLogCollector collector = new ScheduleRequestLogCollector(json);

    @Test
    void capturesMultipleRequestsAndFailuresWithRedactedBodiesAndSearchableExecutionId() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ScheduleRequestLogCollector.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try (var scope = collector.open("partnerSync", 41L)) {
            var first = ScheduleRequestLogCollector.start("POST",
                    "https://user:password@mdm.test/query?access_token=secret&page=1", Map.of("apiKey", "key"),
                    Map.of("password", "hidden", "pageNo", 1), "application/json");
            first.finish(200, bytes("{\"token\":\"hidden\",\"mobile\":\"13800000000\",\"amount\":12345678901234567890.123456789}"),
                    "application/json", null);
            var second = ScheduleRequestLogCollector.start("GET", "https://mdm.test/query?page=2", null, null, null);
            second.finish(null, null, null, new java.net.SocketTimeoutException("sensitive upstream body"));
            var snapshot = scope.snapshot();
            assertThat(snapshot.totalRequests()).isEqualTo(2);
            assertThat(snapshot.requests()).extracting(ScheduleRequestSnapshot.RequestLog::sequence).containsExactly(1, 2);
            assertThat(snapshot.requests().get(0).url()).isEqualTo("https://mdm.test/query?access_token=***&page=1");
            String encoded = json.writeValueAsString(snapshot);
            assertThat(encoded).contains("12345678901234567890.123456789", "SocketTimeoutException", "***")
                    .doesNotContain("13800000000", "hidden", "apiKey\\\":\\\"key", "sensitive upstream body");
            assertThat(appender.list).isEmpty();
        } finally {
            logger.detachAppender(appender);
        }
        assertThat(ScheduleRequestLogCollector.start("GET", "https://mdm.test", null, null, null)).isNull();
    }

    @Test
    void limitsBodiesAndSnapshotWhileContinuingToCountAndPrintRequests() throws Exception {
        try (var scope = collector.open("large", 42L)) {
            var oversized = ScheduleRequestLogCollector.start("POST", "https://mdm.test/query", null,
                    Map.of("value", "x".repeat(ScheduleRequestLogCollector.MAX_BODY_BYTES)), "application/json");
            oversized.finish(200, bytes("{\"value\":\"" + "x".repeat(ScheduleRequestLogCollector.MAX_BODY_BYTES) + "\"}"),
                    "application/json", null);
            assertThat(scope.snapshot().requests().get(0).responseBody()).contains("omitted", "65536");
            for (int i = 0; i < ScheduleRequestLogCollector.MAX_REQUESTS + 1; i++) {
                var next = ScheduleRequestLogCollector.start("GET", "https://mdm.test/query", null, null, null);
                next.finish(200, bytes("{\"value\":\"" + "x".repeat(16000) + "\"}"), "application/json", null);
            }
            var snapshot = scope.snapshot();
            assertThat(snapshot.totalRequests()).isEqualTo(ScheduleRequestLogCollector.MAX_REQUESTS + 2);
            assertThat(snapshot.requests().size()).isLessThan(snapshot.totalRequests());
            assertThat(json.writeValueAsBytes(snapshot).length).isLessThanOrEqualTo(ScheduleRequestLogCollector.MAX_SNAPSHOT_BYTES);
        }
    }

    @Test
    void scopesDoNotLeakBetweenThreadsExecutionsOrMdc() {
        MDC.put("scheduleExecutionId", "previous");
        try {
            try (var outer = collector.open("outer", 1L)) {
                assertThat(CompletableFuture.supplyAsync(() ->
                        ScheduleRequestLogCollector.start("GET", "https://mdm.test", null, null, null)).join()).isNull();
                try (var inner = collector.open("inner", 2L)) {
                    ScheduleRequestLogCollector.start("GET", "https://mdm.test/inner", null, null, null)
                            .finish(200, null, null, null);
                    assertThat(inner.snapshot().totalRequests()).isEqualTo(1);
                }
                assertThat(outer.snapshot().totalRequests()).isZero();
                assertThat(MDC.get("scheduleExecutionId")).isEqualTo("1");
            }
            assertThat(MDC.get("scheduleExecutionId")).isEqualTo("previous");
        } finally {
            MDC.clear();
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
