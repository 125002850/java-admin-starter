package com.oigit.admin.schedule.infra.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.operator.OperatorContext;
import com.oigit.admin.schedule.enums.ScheduleJobOperationType;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobEntity;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobExecutionLogEntity;
import com.oigit.admin.schedule.infra.persistence.entity.ScheduleJobOperationLogEntity;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobExecutionLogMapper;
import com.oigit.admin.schedule.infra.persistence.mapper.ScheduleJobOperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
                        .orderByDesc(ScheduleJobExecutionLogEntity::getStartTime);
        return executionLogMapper.selectList(wrapper);
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
