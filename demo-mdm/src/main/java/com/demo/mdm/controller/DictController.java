package com.demo.mdm.controller;

import com.demo.core.web.R;
import com.demo.mdm.app.DictAppService;
import com.demo.mdm.controller.dto.DictListReqDTO;
import com.demo.mdm.controller.dto.DictItemRspDTO;
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
@Tag(name = "主数据字典", description = "字典查询相关接口")
@RequestMapping("/api/mdm/dict")
public class DictController {

    private final DictAppService dictAppService;

    public DictController(DictAppService dictAppService) {
        this.dictAppService = dictAppService;
    }

    @Operation(summary = "按字典类型查询字典项", description = "根据租户和字典类型编码返回字典项列表")
    @PostMapping("/items/by-type")
    public R<List<DictItemRspDTO>> listItemsByType(@Valid @RequestBody DictListReqDTO reqDTO) {
        return R.ok(dictAppService.listItemsByType(reqDTO));
    }
}
