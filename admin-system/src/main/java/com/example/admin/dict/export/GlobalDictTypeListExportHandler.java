package com.example.admin.dict.export;

import com.example.admin.core.query.ast.QueryAst;
import com.example.admin.core.query.scene.SceneQueryDefinition;
import com.example.admin.core.query.support.DynamicQueryGuard;
import com.example.admin.core.export.model.ExportColumn;
import com.example.admin.core.export.model.ExportScope;
import com.example.admin.core.export.support.AbstractCsvListExportHandler;
import com.example.admin.dict.controller.dto.query.GlobalDictTypeDynamicCriteriaReqDTO;
import com.example.admin.dict.infra.entity.GlobalDictTypeEntity;
import com.example.admin.dict.query.globaldict.GlobalDictTypeSceneQueryDefinition;
import com.example.admin.dict.query.globaldict.GlobalDictTypeSceneQueryMapper;
import com.example.admin.dict.service.DictService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GlobalDictTypeListExportHandler extends AbstractCsvListExportHandler<GlobalDictTypeDynamicCriteriaReqDTO> {

    private final DictService dictService;
    private final DynamicQueryGuard dynamicQueryGuard;
    private final GlobalDictTypeSceneQueryMapper globalDictTypeSceneQueryMapper;
    private final GlobalDictTypeSceneQueryDefinition globalDictTypeSceneQueryDefinition;

    public GlobalDictTypeListExportHandler(
            DictService dictService,
            DynamicQueryGuard dynamicQueryGuard,
            GlobalDictTypeSceneQueryMapper globalDictTypeSceneQueryMapper,
            GlobalDictTypeSceneQueryDefinition globalDictTypeSceneQueryDefinition
    ) {
        this.dictService = dictService;
        this.dynamicQueryGuard = dynamicQueryGuard;
        this.globalDictTypeSceneQueryMapper = globalDictTypeSceneQueryMapper;
        this.globalDictTypeSceneQueryDefinition = globalDictTypeSceneQueryDefinition;
    }

    @Override
    public String sceneCode() {
        return "mdm.global.dict.type.list";
    }

    @Override
    protected String businessName() {
        return "全局字典类型";
    }

    @Override
    public Class<GlobalDictTypeDynamicCriteriaReqDTO> queryType() {
        return GlobalDictTypeDynamicCriteriaReqDTO.class;
    }

    @Override
    public void validate(GlobalDictTypeDynamicCriteriaReqDTO query) {
        QueryAst queryAst = globalDictTypeSceneQueryMapper.toQueryAst(query);
        dynamicQueryGuard.validate(queryAst, globalDictTypeSceneQueryDefinition.maxComplexityScore());
    }

    @Override
    protected ExportScope resolveExportScope(GlobalDictTypeDynamicCriteriaReqDTO query) {
        return query.getCondition() == null ? ExportScope.allData() : ExportScope.dynamicQuery();
    }

    @Override
    public QueryAst summaryQueryAst(GlobalDictTypeDynamicCriteriaReqDTO query) {
        return globalDictTypeSceneQueryMapper.toQueryAst(query);
    }

    @Override
    public SceneQueryDefinition<?> summarySceneQueryDefinition() {
        return globalDictTypeSceneQueryDefinition;
    }

    @Override
    public List<ExportColumn> columns(GlobalDictTypeDynamicCriteriaReqDTO query) {
        return List.of(
            new ExportColumn("id", "ID", 1),
            new ExportColumn("dictTypeCode", "字典类型编码", 2),
            new ExportColumn("dictTypeName", "字典类型名称", 3),
            new ExportColumn("createTime", "创建时间", 4),
            new ExportColumn("updateTime", "更新时间", 5)
        );
    }

    @Override
    public List<GlobalDictTypeEntity> queryRows(GlobalDictTypeDynamicCriteriaReqDTO query) {
        QueryAst queryAst = globalDictTypeSceneQueryMapper.toQueryAst(query);
        return dictService.listGlobalTypesForExport(queryAst, globalDictTypeSceneQueryDefinition);
    }
}
