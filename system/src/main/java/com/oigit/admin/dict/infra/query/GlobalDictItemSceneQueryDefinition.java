package com.oigit.admin.dict.infra.query;

import com.oigit.admin.core.query.ast.SortSpec;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.core.query.scene.QueryFieldSpec;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GlobalDictItemSceneQueryDefinition implements SceneQueryDefinition<GlobalDictItemEntity> {

    @Override
    public String sceneCode() {
        return "mdm.global.dict.item.page";
    }

    @Override
    public List<SortSpec> defaultSorts() {
        return List.of(
                new SortSpec("sortOrder", SortItemDTO.SortDirection.ASC),
                new SortSpec("id", SortItemDTO.SortDirection.ASC)
        );
    }

    @Override
    public Map<String, QueryFieldSpec<GlobalDictItemEntity>> fields() {
        return Map.ofEntries(
                Map.entry("dictTypeCode", QueryFieldSpec.text(GlobalDictItemEntity::getDictTypeCode, "字典类型编码")),
                Map.entry("dictItemCode", QueryFieldSpec.text(GlobalDictItemEntity::getDictItemCode, "字典项编码")),
                Map.entry("dictItemName", QueryFieldSpec.text(GlobalDictItemEntity::getDictItemName, "字典项名称")),
                Map.entry("createTime", QueryFieldSpec.dateTime(GlobalDictItemEntity::getCreateTime, "创建时间").sortable()),
                Map.entry("id", QueryFieldSpec.sortOnly(GlobalDictItemEntity::getId, "id")),
                Map.entry("sortOrder", QueryFieldSpec.sortOnly(GlobalDictItemEntity::getSortOrder, "sortOrder"))
        );
    }
}
