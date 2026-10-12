package com.oigit.admin.queryscheme.controller;

import com.oigit.admin.core.web.R;
import com.oigit.admin.queryscheme.app.QuerySchemeAppService;
import com.oigit.admin.queryscheme.dto.req.QuerySchemeCreateReqDTO;
import com.oigit.admin.queryscheme.dto.req.QuerySchemeDefaultUpdateReqDTO;
import com.oigit.admin.queryscheme.dto.req.QuerySchemeDeleteReqDTO;
import com.oigit.admin.queryscheme.dto.req.QuerySchemeListReqDTO;
import com.oigit.admin.queryscheme.dto.req.QuerySchemeUpdateReqDTO;
import com.oigit.admin.queryscheme.dto.rsp.QuerySchemeRspDTO;
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
@Tag(name = "个人查询方案", description = "按当前用户和动态查询场景保存筛选条件")
@RequestMapping("/api/system/query-scheme")
public class QuerySchemeController {
    private final QuerySchemeAppService appService;

    public QuerySchemeController(QuerySchemeAppService appService) {
        this.appService = appService;
    }

    @Operation(summary = "查询当前用户的查询方案", operationId = "listQuerySchemes")
    @PostMapping("/list")
    public R<List<QuerySchemeRspDTO>> list(@Valid @RequestBody QuerySchemeListReqDTO reqDTO) {
        return R.ok(appService.list(reqDTO.getSceneCode()));
    }

    @Operation(summary = "新增个人查询方案", operationId = "createQueryScheme")
    @PostMapping("/create")
    public R<Long> create(@Valid @RequestBody QuerySchemeCreateReqDTO reqDTO) {
        return R.ok(appService.create(reqDTO));
    }

    @Operation(summary = "更新个人查询方案", operationId = "updateQueryScheme")
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody QuerySchemeUpdateReqDTO reqDTO) {
        appService.update(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除个人查询方案", operationId = "deleteQueryScheme")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody QuerySchemeDeleteReqDTO reqDTO) {
        appService.delete(reqDTO.getId(), reqDTO.getVersion());
        return R.ok();
    }

    @Operation(summary = "设置或取消默认查询方案", operationId = "updateDefaultQueryScheme")
    @PostMapping("/default/update")
    public R<Void> updateDefault(@Valid @RequestBody QuerySchemeDefaultUpdateReqDTO reqDTO) {
        appService.updateDefault(reqDTO);
        return R.ok();
    }
}
