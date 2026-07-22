package com.oigit.admin.dict.query.globaldict;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.ast.SortSpec;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.dict.infra.entity.GlobalDictTypeEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GlobalDictTypeSceneQueryDefinition implements SceneQueryDefinition<GlobalDictTypeEntity> {

    @Override
    public String sceneCode() {
        return "mdm.global.dict.type.list";
    }

    @Override
    public Map<String, SFunction<GlobalDictTypeEntity, String>> textFields() {
        return Map.of(
            "dictTypeCode", GlobalDictTypeEntity::getDictTypeCode,
            "dictTypeName", GlobalDictTypeEntity::getDictTypeName
        );
    }

    @Override
    public Map<String, SFunction<GlobalDictTypeEntity, LocalDateTime>> dateTimeFields() {
        return Map.of("createTime", GlobalDictTypeEntity::getCreateTime);
    }

    @Override
    public Map<String, SFunction<GlobalDictTypeEntity, ?>> enumFields() {
        return Map.of();
    }

    @Override
    public Map<String, SFunction<GlobalDictTypeEntity, ?>> sortFields() {
        return Map.of(
            "id", GlobalDictTypeEntity::getId,
            "createTime", GlobalDictTypeEntity::getCreateTime
        );
    }

    @Override
    public Map<String, String> fieldLabels() {
        return Map.of(
            "id", "ID",
            "dictTypeCode", "字典类型编码",
            "dictTypeName", "字典类型名称",
            "createTime", "创建时间"
        );
    }

    @Override
    public Set<QueryOperator> allowedOperators(String fieldKey) {
        return switch (fieldKey) {
            case "dictTypeCode", "dictTypeName" -> Set.of(
                QueryOperator.EQ,
                QueryOperator.CONTAINS,
                QueryOperator.STARTS_WITH,
                QueryOperator.ENDS_WITH,
                QueryOperator.IN,
                QueryOperator.IS_NULL,
                QueryOperator.IS_NOT_NULL
            );
            case "createTime" -> Set.of(
                QueryOperator.GT,
                QueryOperator.GTE,
                QueryOperator.LT,
                QueryOperator.LTE,
                QueryOperator.BETWEEN,
                QueryOperator.IS_NULL,
                QueryOperator.IS_NOT_NULL
            );
            default -> Set.of();
        };
    }

    @Override
    public List<SortSpec> defaultSorts() {
        return List.of(new SortSpec("id", SortItemDTO.SortDirection.ASC));
    }
}
