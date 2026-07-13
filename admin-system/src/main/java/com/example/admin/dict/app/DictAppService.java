package com.example.admin.dict.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.core.mybatis.BaseEntity;
import com.example.admin.core.operator.OperatorUsernameResolver;
import com.example.admin.core.query.scene.DynamicQueryAstMapper;
import com.example.admin.core.query.ast.QueryAst;
import com.example.admin.core.query.support.DynamicQueryGuard;
import com.example.admin.core.web.PageResult;
import com.example.admin.dict.controller.dto.DictItemRspDTO;
import com.example.admin.dict.controller.dto.GlobalDictItemCreateReqDTO;
import com.example.admin.dict.controller.dto.GlobalDictItemDeleteReqDTO;
import com.example.admin.dict.controller.dto.GlobalDictItemUpdateReqDTO;
import com.example.admin.dict.controller.dto.GlobalDictTypeCreateReqDTO;
import com.example.admin.dict.controller.dto.GlobalDictTypeDeleteReqDTO;
import com.example.admin.dict.controller.dto.GlobalDictTypeListReqDTO;
import com.example.admin.dict.controller.dto.GlobalDictTypeRspDTO;
import com.example.admin.dict.controller.dto.GlobalDictTypeUpdateReqDTO;
import com.example.admin.dict.controller.dto.query.GlobalDictItemDynamicPageReqDTO;
import com.example.admin.dict.controller.dto.query.GlobalDictTypeDynamicListReqDTO;
import com.example.admin.dict.infra.entity.GlobalDictItemEntity;
import com.example.admin.dict.infra.entity.GlobalDictTypeEntity;
import com.example.admin.dict.query.globaldict.GlobalDictItemSceneQueryDefinition;
import com.example.admin.dict.query.globaldict.GlobalDictItemSceneQueryMapper;
import com.example.admin.dict.query.globaldict.GlobalDictTypeSceneQueryDefinition;
import com.example.admin.dict.query.globaldict.GlobalDictTypeSceneQueryMapper;
import com.example.admin.dict.service.DictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DictAppService {

    private final DictService dictService;
    private final DynamicQueryGuard dynamicQueryGuard;
    private final GlobalDictTypeSceneQueryMapper globalDictTypeSceneQueryMapper;
    private final GlobalDictTypeSceneQueryDefinition globalDictTypeSceneQueryDefinition;
    private final GlobalDictItemSceneQueryMapper globalDictItemSceneQueryMapper;
    private final GlobalDictItemSceneQueryDefinition globalDictItemSceneQueryDefinition;
    private final OperatorUsernameResolver operatorUsernameResolver;

    public DictAppService(
            DictService dictService,
            DynamicQueryGuard dynamicQueryGuard,
            GlobalDictTypeSceneQueryMapper globalDictTypeSceneQueryMapper,
            GlobalDictTypeSceneQueryDefinition globalDictTypeSceneQueryDefinition,
            GlobalDictItemSceneQueryMapper globalDictItemSceneQueryMapper,
            GlobalDictItemSceneQueryDefinition globalDictItemSceneQueryDefinition,
            OperatorUsernameResolver operatorUsernameResolver
    ) {
        this.dictService = dictService;
        this.dynamicQueryGuard = dynamicQueryGuard;
        this.globalDictTypeSceneQueryMapper = globalDictTypeSceneQueryMapper;
        this.globalDictTypeSceneQueryDefinition = globalDictTypeSceneQueryDefinition;
        this.globalDictItemSceneQueryMapper = globalDictItemSceneQueryMapper;
        this.globalDictItemSceneQueryDefinition = globalDictItemSceneQueryDefinition;
        this.operatorUsernameResolver = operatorUsernameResolver;
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
        Map<Long, String> usernames = auditUsernames(page.getRecords());
        return new PageResult<>(page.getRecords().stream()
                .map(type -> toGlobalTypeRsp(type, usernames))
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
        Map<Long, String> usernames = auditUsernames(types);
        return types.stream()
                .map(type -> toGlobalTypeRsp(type, usernames))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResult<DictItemRspDTO> listGlobalItemsByType(GlobalDictItemDynamicPageReqDTO reqDTO) {
        QueryAst queryAst = DynamicQueryAstMapper.toPageQueryAst(reqDTO);
        dynamicQueryGuard.validate(queryAst, globalDictItemSceneQueryDefinition.maxComplexityScore());
        Page<GlobalDictItemEntity> page = dictService.pageGlobalItems(queryAst, globalDictItemSceneQueryDefinition);
        Map<Long, String> usernames = auditUsernames(page.getRecords());
        return new PageResult<>(page.getRecords().stream()
                .map(item -> toDictItemRsp(item, usernames))
                .collect(Collectors.toList()), page.getTotal());
    }

    private GlobalDictTypeRspDTO toGlobalTypeRsp(
            GlobalDictTypeEntity type,
            Map<Long, String> usernames
    ) {
        GlobalDictTypeRspDTO dto = new GlobalDictTypeRspDTO(
                type.getId(), type.getDictTypeCode(), type.getDictTypeName());
        dto.setRemark(type.getRemark());
        dto.setStatus(type.getStatus().getCode());
        dto.setCreateTime(type.getCreateTime());
        dto.setUpdateTime(type.getUpdateTime());
        dto.setCreateBy(auditUsername(usernames, type.getCreateBy()));
        dto.setUpdateBy(auditUsername(usernames, type.getUpdateBy()));
        return dto;
    }

    private DictItemRspDTO toDictItemRsp(GlobalDictItemEntity item, Map<Long, String> usernames) {
        DictItemRspDTO dto = new DictItemRspDTO(
                item.getId(), item.getDictTypeCode(), item.getDictItemCode(), item.getDictItemName());
        dto.setSortOrder(item.getSortOrder());
        dto.setRemark(item.getRemark());
        dto.setStatus(item.getStatus().getCode());
        dto.setCreateTime(item.getCreateTime());
        dto.setUpdateTime(item.getUpdateTime());
        dto.setCreateBy(auditUsername(usernames, item.getCreateBy()));
        dto.setUpdateBy(auditUsername(usernames, item.getUpdateBy()));
        return dto;
    }

    private Map<Long, String> auditUsernames(List<? extends BaseEntity> entities) {
        return operatorUsernameResolver.resolveUsernames(entities.stream()
                .flatMap(entity -> Stream.of(entity.getCreateBy(), entity.getUpdateBy()))
                .toList());
    }

    private String auditUsername(Map<Long, String> usernames, Long operatorId) {
        return operatorId == null ? null : usernames.get(operatorId);
    }
}
