package com.demo.mdm.controller;

import com.demo.core.web.PageResult;
import com.demo.core.web.R;
import com.demo.mdm.app.DictAppService;
import com.demo.mdm.controller.dto.DictItemRspDTO;
import com.demo.mdm.controller.dto.GlobalDictItemCreateReqDTO;
import com.demo.mdm.controller.dto.GlobalDictItemDeleteReqDTO;
import com.demo.mdm.controller.dto.GlobalDictItemUpdateReqDTO;
import com.demo.mdm.controller.dto.GlobalDictListReqDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeCreateReqDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeDeleteReqDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeListReqDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeRspDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeUpdateReqDTO;
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
@Tag(name = "全局字典", description = "平台级全局字典维护与查询相关接口")
@RequestMapping("/api/mdm/dict/global")
public class GlobalDictController {

    private final DictAppService dictAppService;

    public GlobalDictController(DictAppService dictAppService) {
        this.dictAppService = dictAppService;
    }

    @Operation(summary = "查询全局字典列表", description = "查询平台级全局字典类型列表")
    @PostMapping("/types/list")
    public R<PageResult<GlobalDictTypeRspDTO>> listGlobalTypes(@Valid @RequestBody GlobalDictTypeListReqDTO reqDTO) {
        return R.ok(dictAppService.listGlobalTypes(reqDTO));
    }

    @Operation(summary = "新增全局字典类型", description = "新增平台级全局字典类型")
    @PostMapping("/type/create")
    public R<Void> createGlobalType(@Valid @RequestBody GlobalDictTypeCreateReqDTO reqDTO) {
        dictAppService.createGlobalType(reqDTO);
        return R.ok();
    }

    @Operation(summary = "修改全局字典类型", description = "修改平台级全局字典类型并同步字典项类型编码")
    @PostMapping("/type/update")
    public R<Void> updateGlobalType(@Valid @RequestBody GlobalDictTypeUpdateReqDTO reqDTO) {
        dictAppService.updateGlobalType(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除全局字典类型", description = "删除平台级全局空字典类型")
    @PostMapping("/type/delete")
    public R<Void> deleteGlobalType(@Valid @RequestBody GlobalDictTypeDeleteReqDTO reqDTO) {
        dictAppService.deleteGlobalType(reqDTO);
        return R.ok();
    }

    @Operation(summary = "新增全局字典项", description = "新增平台级全局字典项")
    @PostMapping("/item/create")
    public R<Void> createGlobalItem(@Valid @RequestBody GlobalDictItemCreateReqDTO reqDTO) {
        dictAppService.createGlobalItem(reqDTO);
        return R.ok();
    }

    @Operation(summary = "修改全局字典项", description = "修改平台级全局字典项，可切换所属字典类型")
    @PostMapping("/item/update")
    public R<Void> updateGlobalItem(@Valid @RequestBody GlobalDictItemUpdateReqDTO reqDTO) {
        dictAppService.updateGlobalItem(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除全局字典项", description = "删除平台级全局字典项")
    @PostMapping("/item/delete")
    public R<Void> deleteGlobalItem(@Valid @RequestBody GlobalDictItemDeleteReqDTO reqDTO) {
        dictAppService.deleteGlobalItem(reqDTO);
        return R.ok();
    }

    @Operation(summary = "按字典类型查询全局字典项", description = "根据全局字典类型编码返回平台级字典项列表")
    @PostMapping("/items/by-type")
    public R<List<DictItemRspDTO>> listGlobalItemsByType(@Valid @RequestBody GlobalDictListReqDTO reqDTO) {
        return R.ok(dictAppService.listGlobalItemsByType(reqDTO));
    }
}
