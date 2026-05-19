package com.demo.mdm.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.core.web.PageResult;
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
import com.demo.mdm.infra.entity.GlobalDictItemEntity;
import com.demo.mdm.infra.entity.GlobalDictTypeEntity;
import com.demo.mdm.service.DictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictAppService {

    private final DictService dictService;

    public DictAppService(DictService dictService) {
        this.dictService = dictService;
    }

    @Transactional
    public void createGlobalType(GlobalDictTypeCreateReqDTO reqDTO) {
        dictService.createGlobalType(reqDTO.getDictTypeCode(), reqDTO.getDictTypeName());
    }

    @Transactional(readOnly = true)
    public PageResult<GlobalDictTypeRspDTO> listGlobalTypes(GlobalDictTypeListReqDTO reqDTO) {
        Page<GlobalDictTypeEntity> page = dictService.listGlobalTypes(reqDTO.getKeyword(), reqDTO.getPageNo(), reqDTO.getPageSize());
        PageResult<GlobalDictTypeRspDTO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setList(page.getRecords().stream()
                .map(type -> new GlobalDictTypeRspDTO(type.getId(), type.getDictTypeCode(), type.getDictTypeName()))
                .collect(Collectors.toList()));
        return result;
    }

    @Transactional
    public void updateGlobalType(GlobalDictTypeUpdateReqDTO reqDTO) {
        dictService.updateGlobalType(reqDTO.getId(), reqDTO.getDictTypeCode(), reqDTO.getDictTypeName());
    }

    @Transactional
    public void deleteGlobalType(GlobalDictTypeDeleteReqDTO reqDTO) {
        dictService.deleteGlobalType(reqDTO.getId());
    }

    @Transactional
    public void createGlobalItem(GlobalDictItemCreateReqDTO reqDTO) {
        dictService.createGlobalItem(reqDTO.getDictTypeCode(), reqDTO.getDictItemCode(), reqDTO.getDictItemName());
    }

    @Transactional
    public void updateGlobalItem(GlobalDictItemUpdateReqDTO reqDTO) {
        dictService.updateGlobalItem(reqDTO.getId(), reqDTO.getDictTypeCode(), reqDTO.getDictItemCode(), reqDTO.getDictItemName());
    }

    @Transactional
    public void deleteGlobalItem(GlobalDictItemDeleteReqDTO reqDTO) {
        dictService.deleteGlobalItem(reqDTO.getId());
    }

    @Transactional(readOnly = true)
    public List<DictItemRspDTO> listGlobalItemsByType(GlobalDictListReqDTO reqDTO) {
        List<GlobalDictItemEntity> items = dictService.listGlobalItemsByType(reqDTO.getDictTypeCode());
        return items.stream()
                .map(item -> new DictItemRspDTO(item.getId(), item.getDictTypeCode(), item.getDictItemCode(), item.getDictItemName()))
                .collect(Collectors.toList());
    }
}
