package com.demo.dict.controller;

import com.demo.core.web.PageResult;
import com.demo.core.web.R;
import com.demo.dict.app.DictAppService;
import com.demo.dict.controller.dto.DictItemRspDTO;
import com.demo.dict.controller.dto.GlobalDictItemCreateReqDTO;
import com.demo.dict.controller.dto.GlobalDictItemDeleteReqDTO;
import com.demo.dict.controller.dto.GlobalDictItemUpdateReqDTO;
import com.demo.dict.controller.dto.GlobalDictTypeCreateReqDTO;
import com.demo.dict.controller.dto.GlobalDictTypeDeleteReqDTO;
import com.demo.dict.controller.dto.GlobalDictTypeListReqDTO;
import com.demo.dict.controller.dto.GlobalDictTypeRspDTO;
import com.demo.dict.controller.dto.GlobalDictTypeUpdateReqDTO;
import com.demo.dict.controller.dto.query.GlobalDictItemDynamicPageReqDTO;
import com.demo.dict.controller.dto.query.GlobalDictTypeDynamicListReqDTO;
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
    public R<PageResult<GlobalDictTypeRspDTO>> listGlobalTypes(@Valid @RequestBody GlobalDictTypeDynamicListReqDTO reqDTO) {
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

    @Operation(summary = "删除全局字典项", description = "批量删除平台级全局字典项，传入ID列表")
    @PostMapping("/item/delete")
    public R<Void> deleteGlobalItem(@Valid @RequestBody GlobalDictItemDeleteReqDTO reqDTO) {
        dictAppService.deleteGlobalItem(reqDTO);
        return R.ok();
    }

    @Operation(summary = "查询全部全局字典类型", description = "查询平台级全局字典类型全部列表（不分页）")
    @PostMapping("/types/list-all")
    public R<List<GlobalDictTypeRspDTO>> listAllGlobalTypes(@Valid @RequestBody GlobalDictTypeListReqDTO reqDTO) {
        return R.ok(dictAppService.listAllGlobalTypes(reqDTO));
    }

    @Operation(summary = "按字典类型查询全局字典项", description = "根据全局字典类型编码分页查询平台级字典项列表")
    @PostMapping("/items/by-type")
    public R<PageResult<DictItemRspDTO>> listGlobalItemsByType(@Valid @RequestBody GlobalDictItemDynamicPageReqDTO reqDTO) {
        return R.ok(dictAppService.listGlobalItemsByType(reqDTO));
    }
}
