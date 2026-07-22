package com.oigit.admin.mdm.dict.query.globaldict;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.ast.SortSpec;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.mdm.dict.infra.entity.GlobalDictItemEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GlobalDictItemSceneQueryDefinition implements SceneQueryDefinition<GlobalDictItemEntity> {

    @Override
    public String sceneCode() {
        return "mdm.global.dict.item.page";
    }

    @Override
    public Map<String, SFunction<GlobalDictItemEntity, String>> textFields() {
        return Map.of(
            "dictTypeCode", GlobalDictItemEntity::getDictTypeCode,
            "dictItemCode", GlobalDictItemEntity::getDictItemCode,
            "dictItemName", GlobalDictItemEntity::getDictItemName
        );
    }

    @Override
    public Map<String, SFunction<GlobalDictItemEntity, LocalDateTime>> dateTimeFields() {
        return Map.of("createTime", GlobalDictItemEntity::getCreateTime);
    }

    @Override
    public Map<String, SFunction<GlobalDictItemEntity, ?>> enumFields() {
        return Map.of();
    }

    @Override
    public Map<String, SFunction<GlobalDictItemEntity, ?>> sortFields() {
        return Map.of(
            "id", GlobalDictItemEntity::getId,
            "sortOrder", GlobalDictItemEntity::getSortOrder,
            "createTime", GlobalDictItemEntity::getCreateTime
        );
    }

    @Override
    public Set<QueryOperator> allowedOperators(String fieldKey) {
        return switch (fieldKey) {
            case "dictTypeCode", "dictItemCode", "dictItemName" -> Set.of(
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
        return List.of(
            new SortSpec("sortOrder", SortItemDTO.SortDirection.ASC),
            new SortSpec("id", SortItemDTO.SortDirection.ASC)
        );
    }
}
