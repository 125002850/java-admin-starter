package com.demo.iam.controller;

import com.demo.core.web.PageResult;
import com.demo.core.web.R;
import com.demo.iam.annotation.RequiresPermission;
import com.demo.iam.app.LogAppService;
import com.demo.iam.dto.IamLogDTO.LogIdReqDTO;
import com.demo.iam.dto.IamLogDTO.LoginLogPageReqDTO;
import com.demo.iam.dto.IamLogDTO.LoginLogRspDTO;
import com.demo.iam.dto.IamLogDTO.OperationLogPageReqDTO;
import com.demo.iam.dto.IamLogDTO.OperationLogRspDTO;
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
@Tag(name = "IAM日志", description = "本地 IAM 登录日志与操作日志查询接口")
@RequestMapping("/api/iam/log")
public class LogController {

    private final LogAppService logAppService;

    public LogController(LogAppService logAppService) {
        this.logAppService = logAppService;
    }

    @RequiresPermission("iam:log:login:query")
    @Operation(summary = "登录日志分页", operationId = "iamLoginLogPage")
    @PostMapping("/login/page")
    public R<PageResult<LoginLogRspDTO>> pageLoginLogs(@Valid @RequestBody LoginLogPageReqDTO reqDTO) {
        return R.ok(logAppService.pageLoginLogs(reqDTO));
    }

    @RequiresPermission("iam:log:login:query")
    @Operation(summary = "登录日志详情", operationId = "iamLoginLogDetail")
    @PostMapping("/login/detail")
    public R<LoginLogRspDTO> loginLogDetail(@Valid @RequestBody LogIdReqDTO reqDTO) {
        return R.ok(logAppService.loginLogDetail(reqDTO));
    }

    @RequiresPermission("iam:log:operation:query")
    @Operation(summary = "操作日志分页", operationId = "iamOperationLogPage")
    @PostMapping("/operation/page")
    public R<PageResult<OperationLogRspDTO>> pageOperationLogs(@Valid @RequestBody OperationLogPageReqDTO reqDTO) {
        return R.ok(logAppService.pageOperationLogs(reqDTO));
    }

    @RequiresPermission("iam:log:operation:query")
    @Operation(summary = "操作日志详情", operationId = "iamOperationLogDetail")
    @PostMapping("/operation/detail")
    public R<OperationLogRspDTO> operationLogDetail(@Valid @RequestBody LogIdReqDTO reqDTO) {
        return R.ok(logAppService.operationLogDetail(reqDTO));
    }
}
