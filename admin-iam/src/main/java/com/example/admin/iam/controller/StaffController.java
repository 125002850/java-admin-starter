package com.example.admin.iam.controller;

import com.example.admin.core.web.PageResult;
import com.example.admin.core.web.R;
import com.example.admin.iam.annotation.RequiresPermission;
import com.example.admin.iam.app.StaffAppService;
import com.example.admin.iam.dto.IamStaffDTO.StaffCreateReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffIdReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffPageReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffPasswordResetReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffRolesAssignReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffRspDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffStatusUpdateReqDTO;
import com.example.admin.iam.dto.IamStaffDTO.StaffUpdateReqDTO;
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
@Tag(name = "IAM员工管理", description = "本地 IAM 员工维护接口")
@RequestMapping("/api/iam/staff")
public class StaffController {

    private final StaffAppService staffAppService;

    public StaffController(StaffAppService staffAppService) {
        this.staffAppService = staffAppService;
    }

    @RequiresPermission("iam:staff:query")
    @Operation(summary = "员工分页", operationId = "iamStaffPage")
    @PostMapping("/page")
    public R<PageResult<StaffRspDTO>> page(@Valid @RequestBody StaffPageReqDTO reqDTO) {
        return R.ok(staffAppService.page(reqDTO));
    }

    @RequiresPermission("iam:staff:query")
    @Operation(summary = "员工详情", operationId = "iamStaffDetail")
    @PostMapping("/detail")
    public R<StaffRspDTO> detail(@Valid @RequestBody StaffIdReqDTO reqDTO) {
        return R.ok(staffAppService.detail(reqDTO.getStaffId()));
    }

    @RequiresPermission("iam:staff:create")
    @Operation(summary = "新增员工", operationId = "iamStaffCreate")
    @PostMapping("/create")
    public R<Void> create(@Valid @RequestBody StaffCreateReqDTO reqDTO) {
        staffAppService.create(reqDTO);
        return R.ok();
    }

    @RequiresPermission("iam:staff:update")
    @Operation(summary = "编辑员工", operationId = "iamStaffUpdate")
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody StaffUpdateReqDTO reqDTO) {
        staffAppService.update(reqDTO);
        return R.ok();
    }

    @RequiresPermission("iam:staff:delete")
    @Operation(summary = "删除员工", operationId = "iamStaffDelete")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody StaffIdReqDTO reqDTO) {
        staffAppService.delete(reqDTO.getStaffId());
        return R.ok();
    }

    @RequiresPermission("iam:staff:update")
    @Operation(summary = "启用或禁用员工", operationId = "iamStaffStatusUpdate")
    @PostMapping("/status/update")
    public R<Void> updateStatus(@Valid @RequestBody StaffStatusUpdateReqDTO reqDTO) {
        staffAppService.updateStatus(reqDTO);
        return R.ok();
    }

    @RequiresPermission("iam:staff:password:reset")
    @Operation(summary = "重置员工密码", operationId = "iamStaffPasswordReset")
    @PostMapping("/password/reset")
    public R<Void> resetPassword(@Valid @RequestBody StaffPasswordResetReqDTO reqDTO) {
        staffAppService.resetPassword(reqDTO);
        return R.ok();
    }

    @RequiresPermission("iam:staff:update")
    @Operation(summary = "分配员工角色", operationId = "iamStaffRolesAssign")
    @PostMapping("/roles/assign")
    public R<Void> assignRoles(@Valid @RequestBody StaffRolesAssignReqDTO reqDTO) {
        staffAppService.assignRoles(reqDTO);
        return R.ok();
    }
}
