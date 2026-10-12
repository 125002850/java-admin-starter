package com.oigit.admin.queryscene.controller;

import com.oigit.admin.core.web.R;
import com.oigit.admin.queryscene.app.QuerySceneAppService;
import com.oigit.admin.queryscene.dto.rsp.QuerySceneMetadataRspDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "动态查询场景", description = "高级搜索字段与条件限制")
@RequestMapping("/api/system/query-scene")
public class QuerySceneController {
    private final QuerySceneAppService appService;

    public QuerySceneController(QuerySceneAppService appService) {
        this.appService = appService;
    }

    @Operation(summary = "获取全部查询场景元数据", description = "按 sceneCode 返回可筛选字段；只描述查询能力，查询执行仍沿用各业务接口的数据权限", operationId = "getQuerySceneMetadata")
    @PostMapping("/metadata")
    public R<Map<String, QuerySceneMetadataRspDTO>> metadata() {
        return R.ok(appService.metadata());
    }
}
