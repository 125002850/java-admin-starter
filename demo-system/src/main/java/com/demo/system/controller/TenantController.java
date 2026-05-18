package com.demo.system.controller;

import com.demo.core.web.R;
import com.demo.system.app.TenantAppService;
import com.demo.system.controller.dto.TenantCreateReqDTO;
import com.demo.system.controller.dto.TenantCreateRspDTO;
import com.demo.system.controller.dto.TenantDeleteReqDTO;
import com.demo.system.controller.dto.TenantUpdateReqDTO;
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
@Tag(name = "租户管理", description = "平台级租户增删改接口")
@RequestMapping("/api/system/tenant")
public class TenantController {

    private final TenantAppService tenantAppService;

    public TenantController(TenantAppService tenantAppService) {
        this.tenantAppService = tenantAppService;
    }

    @Operation(summary = "创建租户", description = "创建新租户，租户名称全局唯一")
    @PostMapping("/create")
    public R<TenantCreateRspDTO> create(@Valid @RequestBody TenantCreateReqDTO reqDTO) {
        return R.ok(tenantAppService.create(reqDTO));
    }

    @Operation(summary = "修改租户", description = "修改租户名称")
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody TenantUpdateReqDTO reqDTO) {
        tenantAppService.update(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除租户", description = "逻辑删除租户，存在未删除用户时拒绝删除")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody TenantDeleteReqDTO reqDTO) {
        tenantAppService.delete(reqDTO);
        return R.ok();
    }
}
