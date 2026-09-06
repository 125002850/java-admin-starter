package com.oigit.admin.iam.controller;

import com.oigit.admin.core.web.R;
import com.oigit.admin.iam.annotation.RequiresPermission;
import com.oigit.admin.iam.app.DeptAppService;
import com.oigit.admin.iam.dto.req.DeptCreateReqDTO;
import com.oigit.admin.iam.dto.req.DeptIdReqDTO;
import com.oigit.admin.iam.dto.req.DeptStatusUpdateReqDTO;
import com.oigit.admin.iam.dto.req.DeptTreeReqDTO;
import com.oigit.admin.iam.dto.req.DeptUpdateReqDTO;
import com.oigit.admin.iam.dto.rsp.DeptRspDTO;

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
@Tag(name = "IAM部门管理", description = "本地 IAM 部门树维护接口")
@RequestMapping("/api/iam/dept")
@RequiresPermission("iam:dept:manage")
public class DeptController {

    private final DeptAppService deptAppService;

    public DeptController(DeptAppService deptAppService) {
        this.deptAppService = deptAppService;
    }

    @Operation(summary = "部门树", operationId = "iamDeptTree")
    @PostMapping("/tree")
    public R<List<DeptRspDTO>> tree(@RequestBody(required = false) DeptTreeReqDTO reqDTO) {
        return R.ok(deptAppService.tree(reqDTO));
    }

    @Operation(summary = "部门详情", operationId = "iamDeptDetail")
    @PostMapping("/detail")
    public R<DeptRspDTO> detail(@Valid @RequestBody DeptIdReqDTO reqDTO) {
        return R.ok(deptAppService.detail(reqDTO.deptId));
    }

    @Operation(summary = "新增部门", operationId = "iamDeptCreate")
    @PostMapping("/create")
    public R<Void> create(@Valid @RequestBody DeptCreateReqDTO reqDTO) {
        deptAppService.create(reqDTO);
        return R.ok();
    }

    @Operation(summary = "编辑部门", operationId = "iamDeptUpdate")
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody DeptUpdateReqDTO reqDTO) {
        deptAppService.update(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除部门", operationId = "iamDeptDelete")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody DeptIdReqDTO reqDTO) {
        deptAppService.delete(reqDTO.deptId);
        return R.ok();
    }

    @Operation(summary = "启用或禁用部门", operationId = "iamDeptStatusUpdate")
    @PostMapping("/status/update")
    public R<Void> updateStatus(@Valid @RequestBody DeptStatusUpdateReqDTO reqDTO) {
        deptAppService.updateStatus(reqDTO);
        return R.ok();
    }
}
