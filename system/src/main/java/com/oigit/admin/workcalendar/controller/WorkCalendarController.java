package com.oigit.admin.workcalendar.controller;

import com.oigit.admin.core.web.R;
import com.oigit.admin.workcalendar.app.WorkCalendarAppService;
import com.oigit.admin.workcalendar.dto.req.ClassifyWorkCalendarTimeReqDTO;
import com.oigit.admin.workcalendar.dto.req.EvaluateWorkCalendarDraftReqDTO;
import com.oigit.admin.workcalendar.dto.req.PrepareWorkCalendarPublicationReqDTO;
import com.oigit.admin.workcalendar.dto.rsp.PrepareWorkCalendarPublicationRspDTO;
import com.oigit.admin.workcalendar.dto.req.PublishWorkCalendarDraftReqDTO;
import com.oigit.admin.workcalendar.dto.req.SaveWorkCalendarDraftReqDTO;
import com.oigit.admin.workcalendar.dto.rsp.TimeClassificationRspDTO;
import com.oigit.admin.workcalendar.dto.req.WorkCalendarYearDetailReqDTO;
import com.oigit.admin.workcalendar.dto.rsp.WorkCalendarYearDetailRspDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@com.oigit.admin.core.audit.OperationAuditModule(code = "system.workcalendar")
@RestController
@Tag(name = "工作日历", description = "平台全局工作日历草稿、发布与时间判定")
@RequestMapping(value = "/api/system/work-calendar", produces = MediaType.APPLICATION_JSON_VALUE)
public class WorkCalendarController {

    private final WorkCalendarAppService appService;

    public WorkCalendarController(WorkCalendarAppService appService) {
        this.appService = appService;
    }

    @Operation(summary = "获取年度工作日历详情", operationId = "fetchWorkCalendarYearDetail")
    @PostMapping("/year/detail")
    public R<WorkCalendarYearDetailRspDTO> fetchYearDetail(
            @Valid @RequestBody WorkCalendarYearDetailReqDTO reqDTO
    ) {
        return R.ok(appService.fetchYearDetail(reqDTO.year(), reqDTO.view()));
    }

    @Operation(summary = "保存工作日历草稿", operationId = "saveWorkCalendarDraft")
    @com.oigit.admin.core.audit.OperationAudit(action = com.oigit.admin.core.audit.OperationAuditAction.UPDATE)
    @PostMapping("/draft/save")
    public R<WorkCalendarYearDetailRspDTO> saveDraft(
            @Valid @RequestBody SaveWorkCalendarDraftReqDTO reqDTO
    ) {
        return R.ok(appService.saveDraft(reqDTO));
    }

    @Operation(summary = "JSON 覆盖导入工作日历草稿特殊日期", operationId = "importWorkCalendarDraft")
    @com.oigit.admin.core.audit.OperationAudit(action = com.oigit.admin.core.audit.OperationAuditAction.IMPORT)
    @PostMapping(value = "/draft/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<WorkCalendarYearDetailRspDTO> importDraft(
            @RequestParam("year") @NotNull @Min(2000) @Max(2099) Integer year,
            @RequestParam("draftVersionId") @NotNull @Min(1) Long draftVersionId,
            @RequestParam("expectedLockVersion") @NotNull @Min(0) Integer expectedLockVersion,
            @Parameter(description = "UTF-8 JSON 文件，最大 1 MiB", required = true)
            @Schema(type = "string", format = "binary")
            @RequestPart("file") MultipartFile file
    ) {
        if (file.isEmpty() || file.getSize() > 1024L * 1024L) {
            throw new com.oigit.admin.core.exception.BizException(com.oigit.admin.workcalendar.enums.WorkCalendarErrorCode.WORK_CALENDAR_IMPORT_INVALID);
        }
        try {
            return R.ok(appService.importDraft(year, draftVersionId, expectedLockVersion, file.getBytes()));
        } catch (java.io.IOException exception) {
            throw new com.oigit.admin.core.exception.BizException(com.oigit.admin.workcalendar.enums.WorkCalendarErrorCode.WORK_CALENDAR_IMPORT_INVALID);
        }
    }

    @Operation(summary = "试算已保存工作日历草稿", operationId = "evaluateWorkCalendarDraft")
    @PostMapping("/draft/evaluate")
    public R<TimeClassificationRspDTO> evaluateDraft(
            @Valid @RequestBody EvaluateWorkCalendarDraftReqDTO reqDTO
    ) {
        return R.ok(appService.evaluateDraft(reqDTO));
    }

    @Operation(summary = "预检工作日历发布", operationId = "prepareWorkCalendarPublication")
    @PostMapping("/publish/prepare")
    public R<PrepareWorkCalendarPublicationRspDTO> preparePublication(
            @Valid @RequestBody PrepareWorkCalendarPublicationReqDTO reqDTO
    ) {
        return R.ok(appService.preparePublication(reqDTO));
    }

    @Operation(summary = "确认发布工作日历草稿", operationId = "publishWorkCalendarDraft")
    @com.oigit.admin.core.audit.OperationAudit(action = com.oigit.admin.core.audit.OperationAuditAction.PUBLISH)
    @PostMapping("/publish")
    public R<WorkCalendarYearDetailRspDTO> publish(
            @Valid @RequestBody PublishWorkCalendarDraftReqDTO reqDTO
    ) {
        return R.ok(appService.publish(reqDTO));
    }

    @Operation(summary = "使用生效工作日历判定时间", operationId = "classifyWorkCalendarTime")
    @PostMapping("/classify")
    public R<TimeClassificationRspDTO> classify(
            @Valid @RequestBody ClassifyWorkCalendarTimeReqDTO reqDTO
    ) {
        return R.ok(appService.classify(reqDTO));
    }
}
