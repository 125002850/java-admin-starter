package com.demo.mdm.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.core.query.scene.DynamicQueryAstMapper;
import com.demo.core.query.ast.QueryAst;
import com.demo.core.query.support.DynamicQueryGuard;
import com.demo.core.web.PageResult;
import com.demo.mdm.controller.dto.DictItemRspDTO;
import com.demo.mdm.controller.dto.GlobalDictItemCreateReqDTO;
import com.demo.mdm.controller.dto.GlobalDictItemDeleteReqDTO;
import com.demo.mdm.controller.dto.GlobalDictItemUpdateReqDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeCreateReqDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeDeleteReqDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeListReqDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeRspDTO;
import com.demo.mdm.controller.dto.GlobalDictTypeUpdateReqDTO;
import com.demo.mdm.controller.dto.query.GlobalDictItemDynamicPageReqDTO;
import com.demo.mdm.controller.dto.query.GlobalDictTypeDynamicListReqDTO;
import com.demo.mdm.infra.entity.GlobalDictItemEntity;
import com.demo.mdm.infra.entity.GlobalDictTypeEntity;
import com.demo.mdm.query.globaldict.GlobalDictItemSceneQueryDefinition;
import com.demo.mdm.query.globaldict.GlobalDictItemSceneQueryMapper;
import com.demo.mdm.query.globaldict.GlobalDictTypeSceneQueryDefinition;
import com.demo.mdm.query.globaldict.GlobalDictTypeSceneQueryMapper;
import com.demo.mdm.service.DictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictAppService {

    private final DictService dictService;
    private final DynamicQueryGuard dynamicQueryGuard;
    private final GlobalDictTypeSceneQueryMapper globalDictTypeSceneQueryMapper;
    private final GlobalDictTypeSceneQueryDefinition globalDictTypeSceneQueryDefinition;
    private final GlobalDictItemSceneQueryMapper globalDictItemSceneQueryMapper;
    private final GlobalDictItemSceneQueryDefinition globalDictItemSceneQueryDefinition;

    public DictAppService(
            DictService dictService,
            DynamicQueryGuard dynamicQueryGuard,
            GlobalDictTypeSceneQueryMapper globalDictTypeSceneQueryMapper,
            GlobalDictTypeSceneQueryDefinition globalDictTypeSceneQueryDefinition,
            GlobalDictItemSceneQueryMapper globalDictItemSceneQueryMapper,
            GlobalDictItemSceneQueryDefinition globalDictItemSceneQueryDefinition
    ) {
        this.dictService = dictService;
        this.dynamicQueryGuard = dynamicQueryGuard;
        this.globalDictTypeSceneQueryMapper = globalDictTypeSceneQueryMapper;
        this.globalDictTypeSceneQueryDefinition = globalDictTypeSceneQueryDefinition;
        this.globalDictItemSceneQueryMapper = globalDictItemSceneQueryMapper;
        this.globalDictItemSceneQueryDefinition = globalDictItemSceneQueryDefinition;
    }

    @Transactional
    public void createGlobalType(GlobalDictTypeCreateReqDTO reqDTO) {
        dictService.createGlobalType(reqDTO.getDictTypeCode(), reqDTO.getDictTypeName());
    }

    @Transactional(readOnly = true)
    public PageResult<GlobalDictTypeRspDTO> listGlobalTypes(GlobalDictTypeDynamicListReqDTO reqDTO) {
        QueryAst queryAst = globalDictTypeSceneQueryMapper.map(reqDTO);
        dynamicQueryGuard.validate(queryAst, globalDictTypeSceneQueryDefinition.maxComplexityScore());
        Page<GlobalDictTypeEntity> page = dictService.pageGlobalTypes(queryAst, globalDictTypeSceneQueryDefinition);
        return new PageResult<>(page.getRecords().stream()
                .map(type -> {
                    GlobalDictTypeRspDTO dto = new GlobalDictTypeRspDTO(type.getId(), type.getDictTypeCode(), type.getDictTypeName());
                    dto.setRemark(type.getRemark());
                    dto.setStatus(type.getStatus().getCode());
                    dto.setCreateTime(type.getCreateTime());
                    dto.setUpdateTime(type.getUpdateTime());
                    dto.setCreateBy(type.getCreateBy());
                    dto.setUpdateBy(type.getUpdateBy());
                    return dto;
                })
                .collect(Collectors.toList()), page.getTotal());
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
        dictService.createGlobalItem(reqDTO.getDictTypeCode(), reqDTO.getDictItemCode(), reqDTO.getDictItemName(), reqDTO.getSortOrder(), reqDTO.getRemark(), reqDTO.getStatus());
    }

    @Transactional
    public void updateGlobalItem(GlobalDictItemUpdateReqDTO reqDTO) {
        dictService.updateGlobalItem(reqDTO.getId(), reqDTO.getDictTypeCode(), reqDTO.getDictItemCode(), reqDTO.getDictItemName(), reqDTO.getSortOrder(), reqDTO.getRemark(), reqDTO.getStatus());
    }

    @Transactional
    public void deleteGlobalItem(GlobalDictItemDeleteReqDTO reqDTO) {
        dictService.deleteGlobalItems(reqDTO.getIds());
    }

    @Transactional(readOnly = true)
    public List<GlobalDictTypeRspDTO> listAllGlobalTypes(GlobalDictTypeListReqDTO reqDTO) {
        List<GlobalDictTypeEntity> types = dictService.listAllGlobalTypes(reqDTO.getKeyword());
        return types.stream()
                .map(type -> {
                    GlobalDictTypeRspDTO dto = new GlobalDictTypeRspDTO(type.getId(), type.getDictTypeCode(), type.getDictTypeName());
                    dto.setRemark(type.getRemark());
                    dto.setStatus(type.getStatus().getCode());
                    dto.setCreateTime(type.getCreateTime());
                    dto.setUpdateTime(type.getUpdateTime());
                    dto.setCreateBy(type.getCreateBy());
                    dto.setUpdateBy(type.getUpdateBy());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResult<DictItemRspDTO> listGlobalItemsByType(GlobalDictItemDynamicPageReqDTO reqDTO) {
        QueryAst queryAst = DynamicQueryAstMapper.toPageQueryAst(reqDTO);
        dynamicQueryGuard.validate(queryAst, globalDictItemSceneQueryDefinition.maxComplexityScore());
        Page<GlobalDictItemEntity> page = dictService.pageGlobalItems(queryAst, globalDictItemSceneQueryDefinition);
        return new PageResult<>(page.getRecords().stream()
                .map(item -> {
                    DictItemRspDTO dto = new DictItemRspDTO(item.getId(), item.getDictTypeCode(), item.getDictItemCode(), item.getDictItemName());
                    dto.setSortOrder(item.getSortOrder());
                    dto.setRemark(item.getRemark());
                    dto.setStatus(item.getStatus().getCode());
                    dto.setCreateTime(item.getCreateTime());
                    dto.setUpdateTime(item.getUpdateTime());
                    dto.setCreateBy(item.getCreateBy());
                    dto.setUpdateBy(item.getUpdateBy());
                    return dto;
                })
                .collect(Collectors.toList()), page.getTotal());
    }
}
