package com.demo.mdm.controller;

import com.demo.core.web.PageResult;
import com.demo.core.web.R;
import com.demo.mdm.app.DictAppService;
import com.demo.mdm.controller.dto.DictListReqDTO;
import com.demo.mdm.controller.dto.DictItemCreateReqDTO;
import com.demo.mdm.controller.dto.DictItemDeleteReqDTO;
import com.demo.mdm.controller.dto.DictItemRspDTO;
import com.demo.mdm.controller.dto.DictItemUpdateReqDTO;
import com.demo.mdm.controller.dto.DictTypeCreateReqDTO;
import com.demo.mdm.controller.dto.DictTypeDeleteReqDTO;
import com.demo.mdm.controller.dto.DictTypeListReqDTO;
import com.demo.mdm.controller.dto.DictTypeRspDTO;
import com.demo.mdm.controller.dto.DictTypeUpdateReqDTO;
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
@Tag(name = "租户字典", description = "租户字典维护与查询相关接口")
@RequestMapping("/api/mdm/dict")
public class TenantDictController {

    private final DictAppService dictAppService;

    public TenantDictController(DictAppService dictAppService) {
        this.dictAppService = dictAppService;
    }

    @Operation(summary = "按字典类型查询字典项", description = "根据租户和字典类型编码返回字典项列表")
    @PostMapping("/items/by-type")
    public R<List<DictItemRspDTO>> listItemsByType(@Valid @RequestBody DictListReqDTO reqDTO) {
        return R.ok(dictAppService.listItemsByType(reqDTO));
    }

    @Operation(summary = "查询租户字典列表", description = "查询当前租户下的字典类型列表")
    @PostMapping("/types/list")
    public R<PageResult<DictTypeRspDTO>> listTypes(@Valid @RequestBody DictTypeListReqDTO reqDTO) {
        return R.ok(dictAppService.listTypes(reqDTO));
    }

    @Operation(summary = "新增租户字典类型", description = "新增当前租户的字典类型")
    @PostMapping("/type/create")
    public R<Void> createType(@Valid @RequestBody DictTypeCreateReqDTO reqDTO) {
        dictAppService.createType(reqDTO);
        return R.ok();
    }

    @Operation(summary = "修改租户字典类型", description = "修改当前租户的字典类型并同步字典项类型编码")
    @PostMapping("/type/update")
    public R<Void> updateType(@Valid @RequestBody DictTypeUpdateReqDTO reqDTO) {
        dictAppService.updateType(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除租户字典类型", description = "删除当前租户的空字典类型")
    @PostMapping("/type/delete")
    public R<Void> deleteType(@Valid @RequestBody DictTypeDeleteReqDTO reqDTO) {
        dictAppService.deleteType(reqDTO);
        return R.ok();
    }

    @Operation(summary = "新增租户字典项", description = "新增当前租户下指定字典类型的字典项")
    @PostMapping("/item/create")
    public R<Void> createItem(@Valid @RequestBody DictItemCreateReqDTO reqDTO) {
        dictAppService.createItem(reqDTO);
        return R.ok();
    }

    @Operation(summary = "修改租户字典项", description = "修改当前租户字典项，可切换所属字典类型")
    @PostMapping("/item/update")
    public R<Void> updateItem(@Valid @RequestBody DictItemUpdateReqDTO reqDTO) {
        dictAppService.updateItem(reqDTO);
        return R.ok();
    }

    @Operation(summary = "删除租户字典项", description = "删除当前租户的字典项")
    @PostMapping("/item/delete")
    public R<Void> deleteItem(@Valid @RequestBody DictItemDeleteReqDTO reqDTO) {
        dictAppService.deleteItem(reqDTO);
        return R.ok();
    }
}
