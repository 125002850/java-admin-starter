package com.oigit.admin.boot.flyway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oigit.admin.core.enums.EnableStatusEnum;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobExecutionLogEntity;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobExecutionLogMapper;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobMapper;
import com.oigit.admin.schedule.infra.scheduling.ScheduleJobManager;
import com.oigit.admin.schedule.infra.persistence.service.ScheduleJobLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ScheduleExecutionIntegrationTests {

    @DynamicPropertySource
    static void isolateDatabase(DynamicPropertyRegistry registry) {
        String url = System.getenv("JAVA_ADMIN_STARTER_DATASOURCE_URL");
        if (url == null || url.isBlank()) {
            registry.add("spring.datasource.url",
                    () -> "jdbc:h2:mem:schedule-execution;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        }
    }

    @MockBean private ScheduleJobManager scheduleJobManager;
    @Autowired private ScheduleJobLogService logs;
    @Autowired private ScheduleJobExecutionLogMapper executionMapper;
    @Autowired private ScheduleJobMapper jobMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private PlatformTransactionManager transactions;
    private final List<Long> jobIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : jobIds) {
            jdbc.update("DELETE FROM sys_schedule_job_execution_log WHERE job_id = ?", id);
            jdbc.update("DELETE FROM sys_schedule_job WHERE id = ?", id);
        }
    }

    @Autowired private com.oigit.admin.schedule.app.ScheduleJobAppService app;

    @Test
    void pageExposesRunningAndCompletedRecord() {
        var job = job();
        var req = new com.oigit.admin.schedule.dto.req.query.ScheduleJobPageQueryReqDTO();
        req.setKeyword(job.getJobCode());
        assertThat(app.page(req).getList().get(0).getLatestExecution()).isNull();
        Long id = logs.startExecution(job);
        var running = app.page(req).getList().get(0).getLatestExecution();
        assertThat(running.getId()).isEqualTo(id);
        assertThat(running.getStatus()).isEqualTo("running");
        assertThat(running.getDurationMillis()).isNotNegative();
        logs.finishExecution(id, "执行失败详情");
        var finished = app.page(req).getList().get(0).getLatestExecution();
        assertThat(finished.getId()).isEqualTo(id);
        assertThat(finished.getStatus()).isEqualTo("failed");
        assertThat(finished.getEndTime()).isNotNull();
        assertThat(finished.getErrorMessage()).isEqualTo("执行失败详情");
    }

    @Test
    void runningRecordIsVisibleBeforeBusinessCompletionAndSurvivesRollback() {
        ScheduleJobEntity job = job();
        Long executionId = new TransactionTemplate(transactions).execute(transaction -> {
            Long id = logs.startExecution(job);
            ScheduleJobExecutionLogEntity visible = CompletableFuture.supplyAsync(
                    () -> executionMapper.selectById(id)).join();
            assertThat(visible.getStatus()).isEqualTo("running");
            assertThat(visible.getStartTime()).isNotNull();
            assertThat(visible.getEndTime()).isNull();
            transaction.setRollbackOnly();
            return id;
        });
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            logs.finishExecution(executionId, "业务回滚");
            transaction.setRollbackOnly();
        });
        var finished = executionMapper.selectById(executionId);
        assertThat(finished.getStatus()).isEqualTo("failed");
        assertThat(finished.getEndTime()).isNotNull();
        assertThat(finished.getErrorMessage()).isEqualTo("业务回滚");
        assertThat(logs.listExecutionLogs(job.getId())).hasSize(1);
        assertThatThrownBy(() -> logs.finishExecution(executionId, null)).isInstanceOf(IllegalStateException.class);
        assertThat(executionMapper.selectById(executionId).getStatus()).isEqualTo("failed");
    }

    @Test
    void latestUsesStartTimeThenIdEvenWhenOlderExecutionFinishesLater() {
        ScheduleJobEntity job = job();
        ScheduleJobEntity empty = job();
        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 10, 0);
        var older = execution(job, start.minusMinutes(1));
        var tied = execution(job, start);
        var latest = execution(job, start);
        logs.finishExecution(tied.getId(), "之前失败");
        logs.finishExecution(older.getId(), null);

        assertThat(logs.latestExecutions(List.of(job.getId(), empty.getId())))
                .hasSize(1).containsKey(job.getId());
        var result = logs.latestExecutions(List.of(job.getId())).get(job.getId());
        assertThat(result.getId()).isEqualTo(latest.getId());
        assertThat(result.getStatus()).isEqualTo("running");
        assertThat(logs.listExecutionLogs(job.getId())).extracting(ScheduleJobExecutionLogEntity::getId)
                .containsExactly(latest.getId(), tied.getId(), older.getId());
        logs.finishExecution(latest.getId(), null);
        assertThat(logs.latestExecutions(List.of(job.getId())).get(job.getId()).getStatus()).isEqualTo("success");
        assertThat(logs.latestExecutions(List.of())).isEmpty();
        jdbc.update("UPDATE sys_schedule_job SET deleted = 1 WHERE id = ?", job.getId());
        assertThat(logs.latestExecutions(List.of(job.getId()))).isEmpty();
    }

    private ScheduleJobEntity job() {
        var job = new ScheduleJobEntity();
        job.setJobCode("schedule-it-" + UUID.randomUUID());
        job.setJobName("执行记录集成测试");
        job.setStatus(EnableStatusEnum.DISABLE);
        jobMapper.insert(job);
        jobIds.add(job.getId());
        return job;
    }

    private ScheduleJobExecutionLogEntity execution(ScheduleJobEntity job, LocalDateTime start) {
        var execution = new ScheduleJobExecutionLogEntity();
        execution.setJobId(job.getId());
        execution.setJobCode(job.getJobCode());
        execution.setJobName(job.getJobName());
        execution.setStartTime(start);
        execution.setStatus("running");
        executionMapper.insert(execution);
        return execution;
    }
}
