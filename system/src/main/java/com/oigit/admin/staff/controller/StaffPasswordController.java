package com.oigit.admin.staff.controller;

import com.oigit.admin.core.web.R;
import com.oigit.admin.staff.app.StaffPasswordAppService;
import com.oigit.admin.staff.dto.req.StaffPasswordChangeReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "账号安全", description = "当前登录人的 SSO 账号安全操作")
@RequestMapping("/api/staff/password")
public class StaffPasswordController {

    private final StaffPasswordAppService appService;

    public StaffPasswordController(StaffPasswordAppService appService) {
        this.appService = appService;
    }

    @Operation(operationId = "changeCurrentStaffPassword", summary = "修改当前登录人的密码", description = "只允许修改当前登录人的密码；密码强度与会话失效由 SSO 决定。"
            + "3004008：旧密码错误；3004009：SSO 业务拒绝；3004011：配置缺失；3004012：配置无效；"
            + "3004013：调用失败；3004014：调用超时；3004015：响应状态异常；3004016：响应格式错误；"
            + "3004017：响应超过64 KiB；3004018：响应编码不支持。"
            + "调用或响应异常不代表密码一定未修改，请先使用新密码登录确认，不要自动重试。")
    @PostMapping("/change")
    public R<Void> change(@Valid @RequestBody StaffPasswordChangeReqDTO request) {
        appService.change(request);
        return R.ok(null);
    }
}
