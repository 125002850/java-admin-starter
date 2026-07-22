package com.oigit.admin.export;

import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.export.service.ExportTaskDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTaskDispatcherTests {

    @AfterEach
    void tearDown() {
        OperatorContext.clear();
        MDC.clear();
    }

    @Test
    void dispatch_should_run_asynchronously_with_caller_context() throws Exception {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("export-dispatch-test-");
        executor.initialize();
        try {
            ExportTaskDispatcher dispatcher = new ExportTaskDispatcher(executor);
            OperatorContext.set(8801L, "exporter", "13800000000", "导出人");
            MDC.put("traceId", "trace-export-1");

            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<String> workerThread = new AtomicReference<>();
            AtomicReference<Long> workerOperatorId = new AtomicReference<>();
            AtomicReference<String> workerOperatorName = new AtomicReference<>();
            AtomicReference<String> workerTraceId = new AtomicReference<>();
            dispatcher.dispatch(() -> {
                workerThread.set(Thread.currentThread().getName());
                workerOperatorId.set(OperatorContext.getOperatorId());
                workerOperatorName.set(OperatorContext.getOperatorName());
                workerTraceId.set(MDC.get("traceId"));
                completed.countDown();
            });

            assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(workerThread.get()).startsWith("export-dispatch-test-");
            assertThat(workerOperatorId.get()).isEqualTo(8801L);
            assertThat(workerOperatorName.get()).isEqualTo("exporter");
            assertThat(workerTraceId.get()).isEqualTo("trace-export-1");
        } finally {
            executor.shutdown();
        }
    }
}
