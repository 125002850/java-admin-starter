package com.oigit.admin.dict.infra.export;

import com.oigit.admin.core.export.model.ExportColumn;
import com.oigit.admin.core.export.model.ExportScope;
import com.oigit.admin.core.export.support.AbstractCsvListExportHandler;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.dict.app.DictAppService;
import com.oigit.admin.dict.dto.req.query.GlobalDictTypeDynamicCriteriaReqDTO;
import com.oigit.admin.dict.domain.model.GlobalDictType;
import com.oigit.admin.dict.infra.query.GlobalDictTypeSceneQueryDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GlobalDictTypeListExportHandler extends AbstractCsvListExportHandler<GlobalDictTypeDynamicCriteriaReqDTO> {

    private final DictAppService dictAppService;
    private final GlobalDictTypeSceneQueryDefinition globalDictTypeSceneQueryDefinition;

    public GlobalDictTypeListExportHandler(
            DictAppService dictAppService,
            GlobalDictTypeSceneQueryDefinition globalDictTypeSceneQueryDefinition
    ) {
        this.dictAppService = dictAppService;
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
        dictAppService.validateGlobalTypeExportQuery(query);
    }

    @Override
    protected ExportScope resolveExportScope(GlobalDictTypeDynamicCriteriaReqDTO query) {
        return query.getCondition() == null ? ExportScope.allData() : ExportScope.dynamicQuery();
    }

    @Override
    public QueryAst summaryQueryAst(GlobalDictTypeDynamicCriteriaReqDTO query) {
        return dictAppService.toGlobalTypeExportQuery(query);
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
                new ExportColumn("statusName", "状态", 4),
                new ExportColumn("createByName", "创建人", 5),
                new ExportColumn("createTime", "创建时间", 6),
                new ExportColumn("updateByName", "更新人", 7),
                new ExportColumn("updateTime", "更新时间", 8)
        );
    }

    @Override
    public List<GlobalDictTypeExportRow> queryRows(GlobalDictTypeDynamicCriteriaReqDTO query) {
        return dictAppService.listGlobalTypesForExport(query).stream().map(this::toExportRow).toList();
    }

    private GlobalDictTypeExportRow toExportRow(GlobalDictType type) {
        GlobalDictTypeExportRow row = new GlobalDictTypeExportRow();
        row.setId(type.getId());
        row.setDictTypeCode(type.getDictTypeCode());
        row.setDictTypeName(type.getDictTypeName());
        row.setStatus(type.getStatus());
        row.setCreateTime(type.getCreateTime());
        row.setUpdateTime(type.getUpdateTime());
        row.setCreateById(type.getCreateBy());
        row.setUpdateById(type.getUpdateBy());
        return row;
    }
}
