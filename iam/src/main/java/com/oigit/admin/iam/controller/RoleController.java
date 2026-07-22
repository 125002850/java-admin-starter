package com.oigit.admin.iam.controller;

import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.core.web.R;
import com.oigit.admin.iam.annotation.RequiresPermission;
import com.oigit.admin.iam.app.RoleAppService;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleCreateReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleDataScopeAssignReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleIdReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleMenusAssignReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RolePageReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleRspDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleStatusUpdateReqDTO;
import com.oigit.admin.iam.dto.IamRoleDTO.RoleUpdateReqDTO;
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
@Tag(name = "IAM角色管理", description = "本地 IAM 角色、菜单授权和数据权限维护接口")
@RequestMapping("/api/iam/role")
@RequiresPermission("iam:role:manage")
public class RoleController {

    private final RoleAppService roleAppService;

    public RoleController(RoleAppService roleAppService) {
        this.roleAppService = roleAppService;
    }

    @Operation(summary = "角色分页", operationId = "iamRolePage")
    @PostMapping("/page")
    public R<PageResult<RoleRspDTO>> page(@Valid @RequestBody RolePageReqDTO reqDTO) {
        return R.ok(roleAppService.page(reqDTO));
    }

    @Operation(summary = "角色详情", operationId = "iamRoleDetail")
    @PostMapping("/detail")
    public R<RoleRspDTO> detail(@Valid @RequestBody RoleIdReqDTO reqDTO) {
        return R.ok(roleAppService.detail(reqDTO.roleId));
    }

    @Operation(summary = "新增角色", operationId = "iamRoleCreate")
    @PostMapping("/create")
    public R<Void> create(@Valid @RequestBody RoleCreateReqDTO reqDTO) {
        roleAppService.create(reqDTO);
        return R.ok();
    }

    @Operation(summary = "编辑角色", operationId = "iamRoleUpdate")
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody RoleUpdateReqDTO reqDTO) {
        roleAppService.update(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除角色", operationId = "iamRoleDelete")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody RoleIdReqDTO reqDTO) {
        roleAppService.delete(reqDTO.roleId);
        return R.ok();
    }

    @Operation(summary = "启用或禁用角色", operationId = "iamRoleStatusUpdate")
    @PostMapping("/status/update")
    public R<Void> updateStatus(@Valid @RequestBody RoleStatusUpdateReqDTO reqDTO) {
        roleAppService.updateStatus(reqDTO);
        return R.ok();
    }

    @Operation(summary = "分配角色菜单权限", operationId = "iamRoleMenusAssign")
    @PostMapping("/menus/assign")
    public R<Void> assignMenus(@Valid @RequestBody RoleMenusAssignReqDTO reqDTO) {
        roleAppService.assignMenus(reqDTO);
        return R.ok();
    }

    @Operation(summary = "分配角色数据权限", operationId = "iamRoleDataScopeAssign")
    @PostMapping("/data-scope/assign")
    public R<Void> assignDataScope(@Valid @RequestBody RoleDataScopeAssignReqDTO reqDTO) {
        roleAppService.assignDataScope(reqDTO);
        return R.ok();
    }
}
