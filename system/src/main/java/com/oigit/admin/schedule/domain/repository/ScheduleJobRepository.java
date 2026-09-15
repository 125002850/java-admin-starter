package com.oigit.admin.schedule.domain.repository;
import com.oigit.admin.schedule.domain.model.*;
import com.oigit.admin.core.query.ast.QueryAst;
import java.util.List;
import java.util.Map;
import com.oigit.admin.schedule.enums.ScheduleJobOperationType;
public interface ScheduleJobRepository {
    record JobPage(List<ScheduleJob> records, long total) {}
    JobPage page(QueryAst query);
    int maxQueryComplexityScore();
    ScheduleJob getById(Long id);
    ScheduleJob create(ScheduleJob job);
    ScheduleJob update(ScheduleJob job);
    void delete(Long id);
    ScheduleJob enable(Long id);
    ScheduleJob disable(Long id);
    List<ScheduleJobExecutionLog> listExecutionLogs(Long jobId);
    List<ScheduleJobOperationLog> listOperationLogs(Long jobId);
    void recordOperationLog(ScheduleJob job, ScheduleJobOperationType type, Map<String,Object> content);
}
