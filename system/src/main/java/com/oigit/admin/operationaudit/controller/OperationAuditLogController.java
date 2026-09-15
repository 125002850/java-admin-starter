package com.oigit.admin.operationaudit.controller;

import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.core.web.R;
import com.oigit.admin.operationaudit.app.OperationAuditLogAppService;
import com.oigit.admin.operationaudit.dto.req.OperationAuditLogIdReqDTO;
import com.oigit.admin.operationaudit.dto.req.query.OperationAuditLogDynamicPageReqDTO;
import com.oigit.admin.operationaudit.dto.rsp.OperationAuditLogDetailRspDTO;
import com.oigit.admin.operationaudit.dto.rsp.OperationAuditLogRspDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "操作日志", description = "全局 API 操作审计查询")
@RequestMapping("/api/system/operation-audit")
public class OperationAuditLogController {

    private final OperationAuditLogAppService appService;

    public OperationAuditLogController(OperationAuditLogAppService appService) {
        this.appService = appService;
    }

    @Operation(summary = "分页查询操作日志", operationId = "pageOperationAuditLogs")
    @PostMapping("/page")
    public R<PageResult<OperationAuditLogRspDTO>> page(
            @Valid @RequestBody OperationAuditLogDynamicPageReqDTO reqDTO
    ) {
        return R.ok(appService.page(reqDTO));
    }

    @Operation(summary = "查询操作日志详情", operationId = "detailOperationAuditLog")
    @PostMapping("/detail")
    public R<OperationAuditLogDetailRspDTO> detail(
            @Valid @RequestBody OperationAuditLogIdReqDTO reqDTO
    ) {
        return R.ok(appService.detail(reqDTO.getLogId()));
    }
}
