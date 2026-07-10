package com.example.admin.staff.controller;

import com.example.admin.core.web.R;
import com.example.admin.iam.annotation.RequiresPermission;
import com.example.admin.staff.app.StaffAppService;
import com.example.admin.staff.controller.dto.StaffInfoRspDTO;
import com.example.admin.staff.controller.dto.query.StaffListAllReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@ConditionalOnProperty(prefix = "platform.sso-staff", name = "enabled", havingValue = "true")
@Tag(name = "员工信息", description = "通过 SSO 服务查询员工信息")
@RequestMapping("/api/staff")
@RequiresPermission("iam:staff:query")
public class StaffController {

    private final StaffAppService staffAppService;

    public StaffController(StaffAppService staffAppService) {
        this.staffAppService = staffAppService;
    }

    @Operation(summary = "查询全部员工列表", description = "通过 SSO 服务查询员工全部列表（不分页），支持关键字模糊匹配和多字段精确筛选")
    @PostMapping("/list-all")
    public R<List<StaffInfoRspDTO>> listAll(@Valid @RequestBody StaffListAllReqDTO req) {
        return R.ok(staffAppService.listAll(req));
    }
}
