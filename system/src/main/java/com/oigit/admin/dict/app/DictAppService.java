package com.oigit.admin.dict.app;

import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.support.DynamicQueryGuard;
import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.dict.app.query.GlobalDictItemSceneQueryMapper;
import com.oigit.admin.dict.app.query.GlobalDictTypeSceneQueryMapper;
import com.oigit.admin.dict.dto.req.GlobalDictItemCreateReqDTO;
import com.oigit.admin.dict.dto.req.GlobalDictItemDeleteReqDTO;
import com.oigit.admin.dict.dto.req.GlobalDictItemUpdateReqDTO;
import com.oigit.admin.dict.dto.req.GlobalDictOptionsReqDTO;
import com.oigit.admin.dict.dto.req.GlobalDictTypeCreateReqDTO;
import com.oigit.admin.dict.dto.req.GlobalDictTypeDeleteReqDTO;
import com.oigit.admin.dict.dto.req.GlobalDictTypeListReqDTO;
import com.oigit.admin.dict.dto.req.GlobalDictTypeUpdateReqDTO;
import com.oigit.admin.dict.dto.req.query.GlobalDictItemDynamicPageReqDTO;
import com.oigit.admin.dict.dto.req.query.GlobalDictTypeDynamicCriteriaReqDTO;
import com.oigit.admin.dict.dto.req.query.GlobalDictTypeDynamicListReqDTO;
import com.oigit.admin.dict.dto.rsp.DictItemRspDTO;
import com.oigit.admin.dict.dto.rsp.DictOptionGroupRspDTO;
import com.oigit.admin.dict.dto.rsp.DictOptionRspDTO;
import com.oigit.admin.dict.dto.rsp.GlobalDictTypeRspDTO;
import com.oigit.admin.dict.domain.model.DictPage;
import com.oigit.admin.dict.domain.model.GlobalDictItem;
import com.oigit.admin.dict.domain.model.GlobalDictType;
import com.oigit.admin.dict.domain.repository.GlobalDictItemRepository;
import com.oigit.admin.dict.domain.repository.GlobalDictTypeRepository;
import com.oigit.admin.dict.domain.service.GlobalDictDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DictAppService {

    private final GlobalDictDomainService globalDictDomainService;
    private final GlobalDictTypeRepository globalDictTypeRepository;
    private final GlobalDictItemRepository globalDictItemRepository;
    private final DynamicQueryGuard dynamicQueryGuard;
    private final GlobalDictTypeSceneQueryMapper globalDictTypeSceneQueryMapper;
    private final GlobalDictItemSceneQueryMapper globalDictItemSceneQueryMapper;

    public DictAppService(
            GlobalDictDomainService globalDictDomainService,
            GlobalDictTypeRepository globalDictTypeRepository,
            GlobalDictItemRepository globalDictItemRepository,
            DynamicQueryGuard dynamicQueryGuard,
            GlobalDictTypeSceneQueryMapper globalDictTypeSceneQueryMapper,
            GlobalDictItemSceneQueryMapper globalDictItemSceneQueryMapper
    ) {
        this.globalDictDomainService = globalDictDomainService;
        this.globalDictTypeRepository = globalDictTypeRepository;
        this.globalDictItemRepository = globalDictItemRepository;
        this.dynamicQueryGuard = dynamicQueryGuard;
        this.globalDictTypeSceneQueryMapper = globalDictTypeSceneQueryMapper;
        this.globalDictItemSceneQueryMapper = globalDictItemSceneQueryMapper;
    }

    @Transactional
    public void createGlobalType(GlobalDictTypeCreateReqDTO reqDTO) {
        globalDictDomainService.createGlobalType(reqDTO.getDictTypeCode(), reqDTO.getDictTypeName());
    }

    @Transactional(readOnly = true)
    public PageResult<GlobalDictTypeRspDTO> listGlobalTypes(GlobalDictTypeDynamicListReqDTO reqDTO) {
        QueryAst queryAst = globalDictTypeSceneQueryMapper.map(reqDTO);
        dynamicQueryGuard.validate(queryAst, globalDictTypeRepository.maxQueryComplexityScore());
        DictPage<GlobalDictType> page = globalDictTypeRepository.page(queryAst);
        return new PageResult<>(page.records().stream()
                .map(this::toGlobalTypeRsp)
                .collect(Collectors.toList()), page.total());
    }

    @Transactional
    public void updateGlobalType(GlobalDictTypeUpdateReqDTO reqDTO) {
        globalDictDomainService.updateGlobalType(reqDTO.getId(), reqDTO.getDictTypeCode(), reqDTO.getDictTypeName());
    }

    @Transactional
    public void deleteGlobalType(GlobalDictTypeDeleteReqDTO reqDTO) {
        globalDictDomainService.deleteGlobalType(reqDTO.getId());
    }

    @Transactional
    public void createGlobalItem(GlobalDictItemCreateReqDTO reqDTO) {
        globalDictDomainService.createGlobalItem(reqDTO.getDictTypeCode(), reqDTO.getDictItemCode(), reqDTO.getDictItemName(), reqDTO.getSortOrder(), reqDTO.getRemark(), reqDTO.getStatus());
    }

    @Transactional
    public void updateGlobalItem(GlobalDictItemUpdateReqDTO reqDTO) {
        globalDictDomainService.updateGlobalItem(reqDTO.getId(), reqDTO.getDictTypeCode(), reqDTO.getDictItemCode(), reqDTO.getDictItemName(), reqDTO.getSortOrder(), reqDTO.getRemark(), reqDTO.getStatus());
    }

    @Transactional
    public void deleteGlobalItem(GlobalDictItemDeleteReqDTO reqDTO) {
        globalDictDomainService.deleteGlobalItems(reqDTO.getIds());
    }

    @Transactional(readOnly = true)
    public List<GlobalDictTypeRspDTO> listAllGlobalTypes(GlobalDictTypeListReqDTO reqDTO) {
        List<GlobalDictType> types = globalDictTypeRepository.listAll(reqDTO.getKeyword());
        return types.stream()
                .map(this::toGlobalTypeRsp)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResult<DictItemRspDTO> listGlobalItemsByType(GlobalDictItemDynamicPageReqDTO reqDTO) {
        QueryAst queryAst = globalDictItemSceneQueryMapper.map(reqDTO);
        dynamicQueryGuard.validate(queryAst, globalDictItemRepository.maxQueryComplexityScore());
        DictPage<GlobalDictItem> page = globalDictItemRepository.page(queryAst);
        return new PageResult<>(page.records().stream()
                .map(this::toDictItemRsp)
                .collect(Collectors.toList()), page.total());
    }

    @Transactional(readOnly = true)
    public List<DictOptionGroupRspDTO> listGlobalOptions(GlobalDictOptionsReqDTO reqDTO) {
        List<String> typeCodes = reqDTO.getDictTypeCodes().stream().distinct().toList();
        Map<String, List<DictOptionRspDTO>> optionsByType = new LinkedHashMap<>();
        typeCodes.forEach(typeCode -> optionsByType.put(typeCode, new java.util.ArrayList<>()));
        for (GlobalDictItem item : globalDictItemRepository.listByTypeCodes(typeCodes)) {
            List<DictOptionRspDTO> options = optionsByType.get(item.getDictTypeCode());
            if (options != null) {
                options.add(new DictOptionRspDTO(
                        item.getDictItemCode(),
                        item.getDictItemName(),
                        item.getStatus().getCode(),
                        item.getSortOrder()
                ));
            }
        }
        return optionsByType.entrySet().stream()
                .map(entry -> new DictOptionGroupRspDTO(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GlobalDictType> listGlobalTypesForExport(GlobalDictTypeDynamicCriteriaReqDTO reqDTO) {
        QueryAst queryAst = toGlobalTypeExportQuery(reqDTO);
        dynamicQueryGuard.validate(queryAst, globalDictTypeRepository.maxQueryComplexityScore());
        return globalDictTypeRepository.listForExport(queryAst);
    }

    public QueryAst toGlobalTypeExportQuery(GlobalDictTypeDynamicCriteriaReqDTO reqDTO) {
        return globalDictTypeSceneQueryMapper.toQueryAst(reqDTO);
    }

    public void validateGlobalTypeExportQuery(GlobalDictTypeDynamicCriteriaReqDTO reqDTO) {
        dynamicQueryGuard.validate(
                toGlobalTypeExportQuery(reqDTO),
                globalDictTypeRepository.maxQueryComplexityScore()
        );
    }

    private GlobalDictTypeRspDTO toGlobalTypeRsp(GlobalDictType type) {
        GlobalDictTypeRspDTO dto = new GlobalDictTypeRspDTO(
                type.getId(), type.getDictTypeCode(), type.getDictTypeName());
        dto.setRemark(type.getRemark());
        dto.setStatus(type.getStatus().getCode());
        dto.setCreateTime(type.getCreateTime());
        dto.setUpdateTime(type.getUpdateTime());
        dto.setCreateById(type.getCreateBy());
        dto.setUpdateById(type.getUpdateBy());
        return dto;
    }

    private DictItemRspDTO toDictItemRsp(GlobalDictItem item) {
        DictItemRspDTO dto = new DictItemRspDTO(
                item.getId(), item.getDictTypeCode(), item.getDictItemCode(), item.getDictItemName());
        dto.setSortOrder(item.getSortOrder());
        dto.setRemark(item.getRemark());
        dto.setStatus(item.getStatus().getCode());
        dto.setCreateTime(item.getCreateTime());
        dto.setUpdateTime(item.getUpdateTime());
        dto.setCreateById(item.getCreateBy());
        dto.setUpdateById(item.getUpdateBy());
        return dto;
    }
}
