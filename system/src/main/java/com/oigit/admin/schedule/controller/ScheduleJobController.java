package com.oigit.admin.schedule.controller;

import com.oigit.admin.core.audit.OperationAudit;
import com.oigit.admin.core.audit.OperationAuditAction;
import com.oigit.admin.core.audit.OperationAuditModule;
import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.core.web.R;
import com.oigit.admin.schedule.app.ScheduleJobAppService;
import com.oigit.admin.schedule.dto.req.ScheduleJobCreateReqDTO;
import com.oigit.admin.schedule.dto.req.ScheduleJobCronPreviewReqDTO;
import com.oigit.admin.schedule.dto.req.ScheduleJobExecutionLogQueryReqDTO;
import com.oigit.admin.schedule.dto.rsp.ScheduleJobExecutionLogRspDTO;
import com.oigit.admin.schedule.dto.req.ScheduleJobIdReqDTO;
import com.oigit.admin.schedule.dto.req.ScheduleJobOperationLogQueryReqDTO;
import com.oigit.admin.schedule.dto.rsp.ScheduleJobOperationLogRspDTO;
import com.oigit.admin.schedule.dto.rsp.ScheduleJobRspDTO;
import com.oigit.admin.schedule.dto.req.ScheduleJobUpdateReqDTO;
import com.oigit.admin.schedule.dto.req.query.ScheduleJobPageQueryReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@OperationAuditModule(code = "system.schedule")
@Tag(name = "定时任务", description = "定时任务管理相关接口")
@RequestMapping("/api/system/schedule/job")
public class ScheduleJobController {

    private final ScheduleJobAppService scheduleJobAppService;

    public ScheduleJobController(ScheduleJobAppService scheduleJobAppService) {
        this.scheduleJobAppService = scheduleJobAppService;
    }

    @Operation(summary = "分页查询定时任务", description = "分页查询定时任务列表")
    @PostMapping("/page")
    public R<PageResult<ScheduleJobRspDTO>> page(@Valid @RequestBody ScheduleJobPageQueryReqDTO reqDTO) {
        return R.ok(scheduleJobAppService.page(reqDTO));
    }

    @Operation(summary = "新增定时任务", description = "新增定时任务，若启用则自动调度")
    @OperationAudit(action = OperationAuditAction.CREATE)
    @PostMapping("/create")
    public R<ScheduleJobRspDTO> create(@Valid @RequestBody ScheduleJobCreateReqDTO reqDTO) {
        return R.ok(scheduleJobAppService.create(reqDTO));
    }

    @Operation(summary = "修改定时任务", description = "修改定时任务，自动取消旧调度并重新调度")
    @OperationAudit(action = OperationAuditAction.UPDATE)
    @PostMapping("/update")
    public R<ScheduleJobRspDTO> update(@Valid @RequestBody ScheduleJobUpdateReqDTO reqDTO) {
        return R.ok(scheduleJobAppService.update(reqDTO));
    }

    @Operation(summary = "删除定时任务", description = "删除定时任务，自动取消调度")
    @OperationAudit(action = OperationAuditAction.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody ScheduleJobIdReqDTO reqDTO) {
        scheduleJobAppService.delete(reqDTO.getId());
        return R.ok();
    }

    @Operation(summary = "启用定时任务", description = "启用定时任务并加入调度")
    @OperationAudit(action = OperationAuditAction.STATUS_CHANGE)
    @PostMapping("/enable")
    public R<ScheduleJobRspDTO> enable(@Valid @RequestBody ScheduleJobIdReqDTO reqDTO) {
        return R.ok(scheduleJobAppService.enable(reqDTO.getId()));
    }

    @Operation(summary = "禁用定时任务", description = "禁用定时任务并取消调度")
    @OperationAudit(action = OperationAuditAction.STATUS_CHANGE)
    @PostMapping("/disable")
    public R<ScheduleJobRspDTO> disable(@Valid @RequestBody ScheduleJobIdReqDTO reqDTO) {
        return R.ok(scheduleJobAppService.disable(reqDTO.getId()));
    }

    @Operation(summary = "立刻执行", description = "立即触发执行一次定时任务，不影响原有cron调度")
    @OperationAudit(action = OperationAuditAction.EXECUTE)
    @PostMapping("/trigger")
    public R<Void> trigger(@Valid @RequestBody ScheduleJobIdReqDTO reqDTO) {
        scheduleJobAppService.trigger(reqDTO.getId());
        return R.ok();
    }

    @Operation(summary = "查询执行记录", description = "按任务ID查询执行记录列表")
    @PostMapping("/execution-logs")
    public R<List<ScheduleJobExecutionLogRspDTO>> listExecutionLogs(
            @Valid @RequestBody ScheduleJobExecutionLogQueryReqDTO reqDTO) {
        return R.ok(scheduleJobAppService.listExecutionLogs(reqDTO));
    }

    @Operation(summary = "查询操作记录", description = "按任务ID查询操作记录列表")
    @PostMapping("/operation-logs")
    public R<List<ScheduleJobOperationLogRspDTO>> listOperationLogs(
            @Valid @RequestBody ScheduleJobOperationLogQueryReqDTO reqDTO) {
        return R.ok(scheduleJobAppService.listOperationLogs(reqDTO));
    }

    @Operation(summary = "预演接下来执行时间", description = "根据 Cron 表达式预演计算接下来的执行时间列表")
    @PostMapping("/next-executions")
    public R<List<String>> previewNextExecutions(
            @Valid @RequestBody ScheduleJobCronPreviewReqDTO reqDTO) {
        return R.ok(scheduleJobAppService.previewNextExecutions(reqDTO));
    }
}
