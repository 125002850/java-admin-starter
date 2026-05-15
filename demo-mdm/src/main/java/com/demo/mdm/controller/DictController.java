package com.demo.mdm.controller;

import com.demo.core.web.R;
import com.demo.mdm.app.DictAppService;
import com.demo.mdm.controller.dto.DictListReqDTO;
import com.demo.mdm.controller.dto.DictItemRspDTO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/mdm/dict")
public class DictController {

    private final DictAppService dictAppService;

    public DictController(DictAppService dictAppService) {
        this.dictAppService = dictAppService;
    }

    @PostMapping("/items/by-type")
    public R<List<DictItemRspDTO>> listItemsByType(@Valid @RequestBody DictListReqDTO reqDTO) {
        return R.ok(dictAppService.listItemsByType(reqDTO));
    }
}
