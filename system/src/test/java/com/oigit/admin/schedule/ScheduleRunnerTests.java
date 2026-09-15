package com.oigit.admin.schedule;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;
import com.oigit.admin.schedule.infra.provider.HttpJobRunner;
import com.oigit.admin.schedule.infra.provider.ReflectJobRunner;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleRunnerTests {
    @Test void httpOriginsDefaultToDenyAndCannotContainCredentials() {
        var job = new ScheduleJobEntity();
        job.setInvokeRoute("http://127.0.0.1:1/internal");
        assertThatThrownBy(() -> new HttpJobRunner("").execute(job)).isInstanceOf(BizException.class);
        job.setInvokeRoute("http://user:password@127.0.0.1:1/internal");
        assertThatThrownBy(() -> new HttpJobRunner("http://user:password@127.0.0.1:1").execute(job)).isInstanceOf(BizException.class);
    }
    @Test void beanRunnerInvokesOnlyExplicitlyAllowedMethods() throws Exception {
        try (var context = new StaticApplicationContext()) {
            var counter = new CounterJob();
            context.getBeanFactory().registerSingleton("counterJob", counter);
            var job = new ScheduleJobEntity(); job.setInvokeRoute("bean://counterJob.run");
            assertThatThrownBy(() -> new ReflectJobRunner(context, "").execute(job)).isInstanceOf(BizException.class);
            assertThat(counter.calls).hasValue(0);
            new ReflectJobRunner(context, "bean://counterJob.run").execute(job);
            assertThat(counter.calls).hasValue(1);
        }
    }
    public static class CounterJob {
        final AtomicInteger calls = new AtomicInteger();
        public void run() { calls.incrementAndGet(); }
    }
}
