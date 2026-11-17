package com.oigit.admin.dict.infra.query;

import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.core.query.scene.QueryFieldSpec;
import com.oigit.admin.dict.infra.persistence.entity.GlobalDictTypeEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GlobalDictTypeSceneQueryDefinition implements SceneQueryDefinition<GlobalDictTypeEntity> {

    @Override
    public String sceneCode() {
        return "mdm.global.dict.type.list";
    }

    @Override
    public Map<String, QueryFieldSpec<GlobalDictTypeEntity>> fields() {
        return Map.ofEntries(
                Map.entry("dictTypeCode", QueryFieldSpec.text(GlobalDictTypeEntity::getDictTypeCode, "字典类型编码")),
                Map.entry("dictTypeName", QueryFieldSpec.text(GlobalDictTypeEntity::getDictTypeName, "字典类型名称")),
                Map.entry("createTime", QueryFieldSpec.dateTime(GlobalDictTypeEntity::getCreateTime, "创建时间").sortable()),
                Map.entry("id", QueryFieldSpec.sortOnly(GlobalDictTypeEntity::getId, "ID"))
        );
    }
}
