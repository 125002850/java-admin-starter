package com.oigit.admin.schedule.infra.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.schedule.enums.ScheduleJobExecutionStatus;
import com.oigit.admin.schedule.enums.ScheduleJobOperationType;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobExecutionLogEntity;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobOperationLogEntity;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobExecutionLogMapper;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobOperationLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleJobLogService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleJobLogService.class);

    private final ScheduleJobExecutionLogMapper executionLogMapper;
    private final ScheduleJobOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public ScheduleJobLogService(
            ScheduleJobExecutionLogMapper executionLogMapper,
            ScheduleJobOperationLogMapper operationLogMapper,
            ObjectMapper objectMapper) {
        this.executionLogMapper = executionLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ScheduleJobExecutionLogEntity> listExecutionLogs(Long jobId) {
        LambdaQueryWrapper<ScheduleJobExecutionLogEntity> wrapper =
                new LambdaQueryWrapper<ScheduleJobExecutionLogEntity>()
                        .eq(ScheduleJobExecutionLogEntity::getJobId, jobId)
                        .orderByDesc(ScheduleJobExecutionLogEntity::getStartTime)
                        .orderByDesc(ScheduleJobExecutionLogEntity::getId);
        return executionLogMapper.selectList(wrapper);
    }

    @Transactional(readOnly = true)
    public Map<Long, ScheduleJobExecutionLogEntity> latestExecutions(List<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }
        return executionLogMapper.selectLatestByJobIds(jobIds).stream()
                .collect(Collectors.toMap(ScheduleJobExecutionLogEntity::getJobId, Function.identity()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long startExecution(ScheduleJobEntity job) {
        ScheduleJobExecutionLogEntity execution = new ScheduleJobExecutionLogEntity();
        execution.setJobId(job.getId());
        execution.setJobCode(job.getJobCode());
        execution.setJobName(job.getJobName());
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus(ScheduleJobExecutionStatus.RUNNING.getCode());
        executionLogMapper.insert(execution);
        return execution.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishExecution(Long executionId, String failure) {
        int updated = executionLogMapper.update(null, new LambdaUpdateWrapper<ScheduleJobExecutionLogEntity>()
                .eq(ScheduleJobExecutionLogEntity::getId, executionId)
                .eq(ScheduleJobExecutionLogEntity::getStatus, ScheduleJobExecutionStatus.RUNNING.getCode())
                .set(ScheduleJobExecutionLogEntity::getStatus, failure == null
                        ? ScheduleJobExecutionStatus.SUCCESS.getCode() : ScheduleJobExecutionStatus.FAILED.getCode())
                .set(ScheduleJobExecutionLogEntity::getEndTime, LocalDateTime.now())
                .set(ScheduleJobExecutionLogEntity::getErrorMessage, failure));
        if (updated != 1) {
            throw new IllegalStateException("Execution log is missing or already finished: " + executionId);
        }
    }

    @Transactional(readOnly = true)
    public List<ScheduleJobOperationLogEntity> listOperationLogs(Long jobId) {
        LambdaQueryWrapper<ScheduleJobOperationLogEntity> wrapper =
                new LambdaQueryWrapper<ScheduleJobOperationLogEntity>()
                        .eq(ScheduleJobOperationLogEntity::getJobId, jobId)
                        .orderByDesc(ScheduleJobOperationLogEntity::getCreateTime);
        return operationLogMapper.selectList(wrapper);
    }

    @Transactional
    public void recordOperationLog(
            ScheduleJobEntity entity,
            ScheduleJobOperationType operationType,
            Map<String, Object> content) {
        ScheduleJobOperationLogEntity logEntity = new ScheduleJobOperationLogEntity();
        logEntity.setJobId(entity.getId());
        logEntity.setJobCode(entity.getJobCode());
        logEntity.setJobName(entity.getJobName());
        logEntity.setOperationType(operationType.name());
        logEntity.setOperatorId(OperatorContext.getOperatorId());
        logEntity.setOperatorName(OperatorContext.getOperatorName());
        try {
            logEntity.setOperationContent(objectMapper.writeValueAsString(content));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize schedule job operation content", ex);
        }
        operationLogMapper.insert(logEntity);
    }

}
