package com.oigit.admin.iam.controller;

import com.oigit.admin.core.web.R;
import com.oigit.admin.iam.annotation.RequiresPermission;
import com.oigit.admin.iam.app.MenuAppService;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuCreateReqDTO;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuIdReqDTO;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuRspDTO;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuStatusUpdateReqDTO;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuTreeReqDTO;
import com.oigit.admin.iam.dto.IamMenuDTO.MenuUpdateReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "IAM菜单管理", description = "本地 IAM 菜单与按钮权限维护接口")
@RequestMapping("/api/iam/menu")
@RequiresPermission("iam:menu:manage")
public class MenuController {

    private final MenuAppService menuAppService;

    public MenuController(MenuAppService menuAppService) {
        this.menuAppService = menuAppService;
    }

    @Operation(summary = "菜单树", operationId = "iamMenuTree")
    @PostMapping("/tree")
    public R<List<MenuRspDTO>> tree(@RequestBody(required = false) MenuTreeReqDTO reqDTO) {
        return R.ok(menuAppService.tree(reqDTO));
    }

    @Operation(summary = "菜单详情", operationId = "iamMenuDetail")
    @PostMapping("/detail")
    public R<MenuRspDTO> detail(@Valid @RequestBody MenuIdReqDTO reqDTO) {
        return R.ok(menuAppService.detail(reqDTO.menuId));
    }

    @Operation(summary = "新增菜单", operationId = "iamMenuCreate")
    @PostMapping("/create")
    public R<Void> create(@Valid @RequestBody MenuCreateReqDTO reqDTO) {
        menuAppService.create(reqDTO);
        return R.ok();
    }

    @Operation(summary = "编辑菜单", operationId = "iamMenuUpdate")
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody MenuUpdateReqDTO reqDTO) {
        menuAppService.update(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除菜单", operationId = "iamMenuDelete")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody MenuIdReqDTO reqDTO) {
        menuAppService.delete(reqDTO.menuId);
        return R.ok();
    }

    @Operation(summary = "启用或禁用菜单", operationId = "iamMenuStatusUpdate")
    @PostMapping("/status/update")
    public R<Void> updateStatus(@Valid @RequestBody MenuStatusUpdateReqDTO reqDTO) {
        menuAppService.updateStatus(reqDTO);
        return R.ok();
    }
}
