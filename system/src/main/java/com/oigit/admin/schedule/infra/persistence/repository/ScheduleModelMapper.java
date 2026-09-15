package com.oigit.admin.schedule.infra.persistence.repository;
import com.oigit.admin.schedule.domain.model.*;
import com.oigit.admin.schedule.infra.persistence.entity.*;
public final class ScheduleModelMapper {
private ScheduleModelMapper() {}
public static ScheduleJob toModel(ScheduleJobEntity source) { if (source == null) return null; ScheduleJob target = new ScheduleJob();
target.setId(source.getId());
target.setJobName(source.getJobName());
target.setJobCode(source.getJobCode());
target.setDefaultCron(source.getDefaultCron());
target.setCronExpression(source.getCronExpression());
target.setInvokeRoute(source.getInvokeRoute());
target.setStatus(source.getStatus());
target.setRemark(source.getRemark());
target.setGroupName(source.getGroupName());
target.setCreateTime(source.getCreateTime());
target.setUpdateTime(source.getUpdateTime());
target.setCreateBy(source.getCreateBy());
target.setUpdateBy(source.getUpdateBy());
target.setDeleted(source.getDeleted());
target.setVersion(source.getVersion());
return target; }
public static ScheduleJobEntity toEntity(ScheduleJob source) { if (source == null) return null; ScheduleJobEntity target = new ScheduleJobEntity();
target.setId(source.getId());
target.setJobName(source.getJobName());
target.setJobCode(source.getJobCode());
target.setDefaultCron(source.getDefaultCron());
target.setCronExpression(source.getCronExpression());
target.setInvokeRoute(source.getInvokeRoute());
target.setStatus(source.getStatus());
target.setRemark(source.getRemark());
target.setGroupName(source.getGroupName());
target.setCreateTime(source.getCreateTime());
target.setUpdateTime(source.getUpdateTime());
target.setCreateBy(source.getCreateBy());
target.setUpdateBy(source.getUpdateBy());
target.setDeleted(source.getDeleted());
target.setVersion(source.getVersion());
return target; }
public static ScheduleJobExecutionLog toModel(ScheduleJobExecutionLogEntity source) { if (source == null) return null; ScheduleJobExecutionLog target = new ScheduleJobExecutionLog();
target.setId(source.getId());
target.setJobId(source.getJobId());
target.setJobCode(source.getJobCode());
target.setJobName(source.getJobName());
target.setStartTime(source.getStartTime());
target.setEndTime(source.getEndTime());
target.setStatus(source.getStatus());
target.setErrorMessage(source.getErrorMessage());
target.setCreateTime(source.getCreateTime());
target.setCreateTime(source.getCreateTime());
return target; }
public static ScheduleJobExecutionLogEntity toEntity(ScheduleJobExecutionLog source) { if (source == null) return null; ScheduleJobExecutionLogEntity target = new ScheduleJobExecutionLogEntity();
target.setId(source.getId());
target.setJobId(source.getJobId());
target.setJobCode(source.getJobCode());
target.setJobName(source.getJobName());
target.setStartTime(source.getStartTime());
target.setEndTime(source.getEndTime());
target.setStatus(source.getStatus());
target.setErrorMessage(source.getErrorMessage());
target.setCreateTime(source.getCreateTime());
target.setCreateTime(source.getCreateTime());
return target; }
public static ScheduleJobOperationLog toModel(ScheduleJobOperationLogEntity source) { if (source == null) return null; ScheduleJobOperationLog target = new ScheduleJobOperationLog();
target.setId(source.getId());
target.setJobId(source.getJobId());
target.setJobCode(source.getJobCode());
target.setJobName(source.getJobName());
target.setOperationType(source.getOperationType());
target.setOperationContent(source.getOperationContent());
target.setOperatorId(source.getOperatorId());
target.setOperatorName(source.getOperatorName());
target.setCreateTime(source.getCreateTime());
target.setCreateTime(source.getCreateTime());
return target; }
public static ScheduleJobOperationLogEntity toEntity(ScheduleJobOperationLog source) { if (source == null) return null; ScheduleJobOperationLogEntity target = new ScheduleJobOperationLogEntity();
target.setId(source.getId());
target.setJobId(source.getJobId());
target.setJobCode(source.getJobCode());
target.setJobName(source.getJobName());
target.setOperationType(source.getOperationType());
target.setOperationContent(source.getOperationContent());
target.setOperatorId(source.getOperatorId());
target.setOperatorName(source.getOperatorName());
target.setCreateTime(source.getCreateTime());
target.setCreateTime(source.getCreateTime());
return target; }
}
