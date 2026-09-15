package com.oigit.admin.schedule.app;


import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.core.enums.EnableStatusEnum;
import com.oigit.admin.core.exception.BizException;


import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.support.DynamicQueryGuard;
import com.oigit.admin.schedule.dto.req.ScheduleJobCreateReqDTO;
import com.oigit.admin.schedule.dto.req.ScheduleJobCronPreviewReqDTO;
import com.oigit.admin.schedule.dto.req.ScheduleJobExecutionLogQueryReqDTO;
import com.oigit.admin.schedule.dto.rsp.ScheduleJobExecutionLogRspDTO;
import com.oigit.admin.schedule.dto.req.ScheduleJobOperationLogQueryReqDTO;
import com.oigit.admin.schedule.dto.rsp.ScheduleJobOperationLogRspDTO;
import com.oigit.admin.schedule.dto.rsp.ScheduleJobRspDTO;
import com.oigit.admin.schedule.dto.req.ScheduleJobUpdateReqDTO;
import com.oigit.admin.schedule.dto.req.query.ScheduleJobPageQueryReqDTO;
import com.oigit.admin.schedule.enums.ScheduleErrorCode;
import com.oigit.admin.schedule.enums.ScheduleJobOperationType;
import com.oigit.admin.schedule.domain.model.ScheduleJob;
import com.oigit.admin.schedule.domain.model.ScheduleJobExecutionLog;
import com.oigit.admin.schedule.domain.model.ScheduleJobOperationLog;

import com.oigit.admin.schedule.app.query.ScheduleJobSceneQueryMapper;
import com.oigit.admin.schedule.domain.gateway.JobScheduler;

import com.oigit.admin.schedule.domain.repository.ScheduleJobRepository;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ScheduleJobAppService {

    private final ScheduleJobRepository scheduleJobService;
    private final JobScheduler scheduleJobManager;
    private final DynamicQueryGuard dynamicQueryGuard;
    private final ScheduleJobSceneQueryMapper scheduleJobSceneQueryMapper;

    public ScheduleJobAppService(
            ScheduleJobRepository scheduleJobService,
            JobScheduler scheduleJobManager,
            DynamicQueryGuard dynamicQueryGuard,
            ScheduleJobSceneQueryMapper scheduleJobSceneQueryMapper,
            @org.springframework.beans.factory.annotation.Value("${platform.schedule.time-zone:Asia/Shanghai}") String timeZone) {
        this.scheduleJobService = scheduleJobService;
        this.scheduleJobManager = scheduleJobManager;
        this.dynamicQueryGuard = dynamicQueryGuard;
        this.scheduleJobSceneQueryMapper = scheduleJobSceneQueryMapper;
        this.scheduleZone = ZoneId.of(timeZone);
    }

    @Transactional(readOnly = true)
    public PageResult<ScheduleJobRspDTO> page(ScheduleJobPageQueryReqDTO reqDTO) {
        QueryAst queryAst = scheduleJobSceneQueryMapper.map(reqDTO);
        dynamicQueryGuard.validate(queryAst, scheduleJobService.maxQueryComplexityScore());
        ScheduleJobRepository.JobPage page = scheduleJobService.page(queryAst);
        List<ScheduleJobRspDTO> records = page.records().stream()
                .map(this::toRsp)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.total());
    }

    @Transactional
    public ScheduleJobRspDTO create(ScheduleJobCreateReqDTO reqDTO) {
        validateCron(reqDTO.getCronExpression(), reqDTO.getDefaultCron());
        ScheduleJob entity = buildEntity(reqDTO);
        entity = scheduleJobService.create(entity);
        scheduleJobService.recordOperationLog(entity, ScheduleJobOperationType.CREATE, toContentMap(entity));
        if (entity.getStatus() == EnableStatusEnum.ENABLE) {
            scheduleJobManager.scheduleJob(entity);
        }
        return toRsp(entity);
    }

    @Transactional
    public ScheduleJobRspDTO update(ScheduleJobUpdateReqDTO reqDTO) {
        validateCron(reqDTO.getCronExpression(), reqDTO.getDefaultCron());
        ScheduleJob oldEntity = scheduleJobService.getById(reqDTO.getId());
        String oldJobCode = oldEntity.getJobCode();
        Map<String, Object> beforeMap = toContentMap(oldEntity);

        ScheduleJob entity = buildUpdateEntity(reqDTO, oldEntity);
        entity = scheduleJobService.update(entity);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("before", beforeMap);
        content.put("after", toContentMap(entity));
        scheduleJobService.recordOperationLog(entity, ScheduleJobOperationType.UPDATE, content);

        scheduleJobManager.cancelJob(oldJobCode);
        if (EnableStatusEnum.ENABLE.equals(entity.getStatus())) {
            scheduleJobManager.scheduleJob(entity);
        }

        return toRsp(entity);
    }

    @Transactional
    public void delete(Long id) {
        ScheduleJob entity = scheduleJobService.getById(id);
        if (entity.getStatus() == EnableStatusEnum.ENABLE) {
            throw new BizException(ScheduleErrorCode.JOB_ENABLED_DELETE_DENIED);
        }
        scheduleJobService.recordOperationLog(entity, ScheduleJobOperationType.DELETE, toContentMap(entity));
        scheduleJobManager.cancelJob(entity.getJobCode());
        scheduleJobService.delete(id);
    }

    @Transactional
    public ScheduleJobRspDTO enable(Long id) {
        ScheduleJob entity = scheduleJobService.enable(id);
        scheduleJobService.recordOperationLog(entity, ScheduleJobOperationType.ENABLE, toContentMap(entity));
        scheduleJobManager.scheduleJob(entity);
        return toRsp(entity);
    }

    @Transactional
    public ScheduleJobRspDTO disable(Long id) {
        ScheduleJob entity = scheduleJobService.disable(id);
        scheduleJobService.recordOperationLog(entity, ScheduleJobOperationType.DISABLE, toContentMap(entity));
        scheduleJobManager.cancelJob(entity.getJobCode());
        return toRsp(entity);
    }

    public void trigger(Long id) {
        ScheduleJob entity = scheduleJobService.getById(id);
        if (entity.getStatus() != EnableStatusEnum.ENABLE) {
            throw new BizException(ScheduleErrorCode.JOB_DISABLED_TRIGGER_DENIED);
        }
        scheduleJobManager.triggerJob(entity);
    }

    @Transactional(readOnly = true)
    public List<ScheduleJobExecutionLogRspDTO> listExecutionLogs(ScheduleJobExecutionLogQueryReqDTO reqDTO) {
        return scheduleJobService.listExecutionLogs(reqDTO.getJobId()).stream()
                .map(this::toExecutionLogRsp)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleJobOperationLogRspDTO> listOperationLogs(ScheduleJobOperationLogQueryReqDTO reqDTO) {
        return scheduleJobService.listOperationLogs(reqDTO.getJobId()).stream()
                .map(this::toOperationLogRsp)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toContentMap(ScheduleJob entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("jobName", entity.getJobName());
        map.put("jobCode", entity.getJobCode());
        map.put("defaultCron", entity.getDefaultCron());
        map.put("cronExpression", entity.getCronExpression());
        map.put("invokeRoute", entity.getInvokeRoute());
        map.put("status", entity.getStatus() != null ? entity.getStatus().getCode() : null);
        map.put("remark", entity.getRemark());
        map.put("groupName", entity.getGroupName());
        return map;
    }

    private void validateCron(String cron, String defaultCron) {
        String effective = StringUtils.hasText(cron) ? cron : defaultCron;
        if (!StringUtils.hasText(effective) || !CronExpression.isValidExpression(effective.trim())) {
            throw new BizException(ScheduleErrorCode.JOB_CRON_INVALID);
        }
    }

    private ScheduleJob buildEntity(ScheduleJobCreateReqDTO reqDTO) {
        ScheduleJob entity = new ScheduleJob();
        entity.setJobName(reqDTO.getJobName());
        entity.setJobCode(reqDTO.getJobCode());
        entity.setDefaultCron(reqDTO.getDefaultCron());
        entity.setCronExpression(reqDTO.getCronExpression());
        entity.setInvokeRoute(reqDTO.getInvokeRoute());
        entity.setStatus(reqDTO.getStatus() != null ? reqDTO.getStatus() : EnableStatusEnum.DISABLE);
        entity.setRemark(reqDTO.getRemark());
        entity.setGroupName(reqDTO.getGroupName());
        return entity;
    }

    private ScheduleJob buildUpdateEntity(ScheduleJobUpdateReqDTO reqDTO, ScheduleJob existing) {
        existing.setJobName(reqDTO.getJobName());
        existing.setJobCode(reqDTO.getJobCode());
        existing.setDefaultCron(reqDTO.getDefaultCron());
        existing.setCronExpression(reqDTO.getCronExpression());
        existing.setInvokeRoute(reqDTO.getInvokeRoute());
        existing.setRemark(reqDTO.getRemark());
        existing.setGroupName(reqDTO.getGroupName());
        return existing;
    }

    private ScheduleJobRspDTO toRsp(ScheduleJob entity) {
        ScheduleJobRspDTO dto = new ScheduleJobRspDTO();
        dto.setId(entity.getId());
        dto.setJobName(entity.getJobName());
        dto.setJobCode(entity.getJobCode());
        dto.setDefaultCron(entity.getDefaultCron());
        dto.setCronExpression(entity.getCronExpression());
        dto.setInvokeRoute(entity.getInvokeRoute());
        dto.setStatus(entity.getStatus().getCode());
        dto.setRemark(entity.getRemark());
        dto.setGroupName(entity.getGroupName());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateById(entity.getCreateBy());
        dto.setUpdateById(entity.getUpdateBy());
        return dto;
    }

    private ScheduleJobExecutionLogRspDTO toExecutionLogRsp(ScheduleJobExecutionLog entity) {
        ScheduleJobExecutionLogRspDTO dto = new ScheduleJobExecutionLogRspDTO();
        dto.setId(entity.getId());
        dto.setJobId(entity.getJobId());
        dto.setJobCode(entity.getJobCode());
        dto.setJobName(entity.getJobName());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setStatus(entity.getStatus());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }

    private final ZoneId scheduleZone;
    private static final DateTimeFormatter EXECUTION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_PREVIEW_COUNT = 5;
    private static final int MAX_PREVIEW_COUNT = 10;

    public List<String> previewNextExecutions(ScheduleJobCronPreviewReqDTO reqDTO) {
        String cron = reqDTO.getCronExpression();
        if (!StringUtils.hasText(cron)) {
            throw new BizException(ScheduleErrorCode.JOB_CRON_INVALID, "Cron 表达式不能为空");
        }
        CronExpression cronExpression;
        try {
            cronExpression = CronExpression.parse(cron.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException(ScheduleErrorCode.JOB_CRON_INVALID, "Cron 表达式格式无效: " + e.getMessage());
        }
        int count = reqDTO.getCount() == null ? DEFAULT_PREVIEW_COUNT : reqDTO.getCount();
        if (count < 1) {
            count = DEFAULT_PREVIEW_COUNT;
        } else if (count > MAX_PREVIEW_COUNT) {
            count = MAX_PREVIEW_COUNT;
        }

        List<String> nextExecutions = new ArrayList<>(count);
        ZonedDateTime nextTime = ZonedDateTime.now(scheduleZone);
        for (int i = 0; i < count; i++) {
            nextTime = cronExpression.next(nextTime);
            if (nextTime == null) {
                break;
            }
            nextExecutions.add(nextTime.format(EXECUTION_TIME_FORMATTER));
        }
        return nextExecutions;
    }

    private ScheduleJobOperationLogRspDTO toOperationLogRsp(ScheduleJobOperationLog entity) {
        ScheduleJobOperationLogRspDTO dto = new ScheduleJobOperationLogRspDTO();
        dto.setId(entity.getId());
        dto.setJobId(entity.getJobId());
        dto.setJobCode(entity.getJobCode());
        dto.setJobName(entity.getJobName());
        dto.setOperationType(entity.getOperationType());
        dto.setOperationContent(entity.getOperationContent());
        dto.setOperatorId(entity.getOperatorId());
        dto.setOperatorName(entity.getOperatorName());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }
}
