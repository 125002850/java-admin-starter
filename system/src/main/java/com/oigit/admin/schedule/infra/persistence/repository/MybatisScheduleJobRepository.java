package com.oigit.admin.schedule.infra.persistence.repository;
import com.oigit.admin.schedule.domain.repository.ScheduleJobRepository;
import com.oigit.admin.schedule.domain.model.*;
import com.oigit.admin.schedule.infra.persistence.service.*;
import com.oigit.admin.schedule.infra.query.ScheduleJobSceneQueryDefinition;
import com.oigit.admin.schedule.enums.ScheduleJobOperationType;
import com.oigit.admin.core.query.ast.QueryAst;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class MybatisScheduleJobRepository implements ScheduleJobRepository {
 private final ScheduleJobService jobs;
 private final ScheduleJobLogService logs;
 private final ScheduleJobSceneQueryDefinition definition;
 public MybatisScheduleJobRepository(ScheduleJobService jobs, ScheduleJobLogService logs, ScheduleJobSceneQueryDefinition definition) {
 this.jobs=jobs; this.logs=logs; this.definition=definition;
 }
 public JobPage page(QueryAst query) { var page=jobs.page(query,definition); return new JobPage(page.getRecords().stream().map(ScheduleModelMapper::toModel).toList(),page.getTotal()); }
 public int maxQueryComplexityScore() { return definition.maxComplexityScore(); }
 public ScheduleJob getById(Long id) { return ScheduleModelMapper.toModel(jobs.getById(id)); }
 public ScheduleJob create(ScheduleJob job) { return ScheduleModelMapper.toModel(jobs.create(ScheduleModelMapper.toEntity(job))); }
 public ScheduleJob update(ScheduleJob job) { return ScheduleModelMapper.toModel(jobs.update(ScheduleModelMapper.toEntity(job))); }
 public void delete(Long id) { jobs.delete(id); }
 public ScheduleJob enable(Long id) { return ScheduleModelMapper.toModel(jobs.enable(id)); }
 public ScheduleJob disable(Long id) { return ScheduleModelMapper.toModel(jobs.disable(id)); }
 public List<ScheduleJobExecutionLog> listExecutionLogs(Long id) { return logs.listExecutionLogs(id).stream().map(ScheduleModelMapper::toModel).toList(); }
 public List<ScheduleJobOperationLog> listOperationLogs(Long id) { return logs.listOperationLogs(id).stream().map(ScheduleModelMapper::toModel).toList(); }
 public void recordOperationLog(ScheduleJob job, ScheduleJobOperationType type, Map<String,Object> content) { logs.recordOperationLog(ScheduleModelMapper.toEntity(job),type,content); }
}
