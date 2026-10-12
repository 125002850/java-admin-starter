package com.oigit.admin.queryscene.app;

import com.oigit.admin.core.query.scene.QueryFieldType;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.core.query.validation.DynamicQueryLimits;
import com.oigit.admin.queryscene.dto.rsp.QuerySceneFieldRspDTO;
import com.oigit.admin.queryscene.dto.rsp.QuerySceneLimitsRspDTO;
import com.oigit.admin.queryscene.dto.rsp.QuerySceneMetadataRspDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class QuerySceneAppService {
    private final Map<String, QuerySceneMetadataRspDTO> metadata;

    public QuerySceneAppService(List<SceneQueryDefinition<?>> scenes) {
        var result = new TreeMap<String, QuerySceneMetadataRspDTO>();
        for (var scene : scenes) {
            var fields = new TreeMap<String, QuerySceneFieldRspDTO>();
            var keys = new java.util.TreeSet<String>();
            keys.addAll(scene.textFields().keySet());
            keys.addAll(scene.dateTimeFields().keySet());
            keys.addAll(scene.enumFields().keySet());
            keys.addAll(scene.booleanFields().keySet());
            keys.addAll(scene.numberFields().keySet());
            keys.addAll(scene.customFieldTypes().keySet());
            for (String key : keys) {
                fields.put(key, new QuerySceneFieldRspDTO(
                        scene.fieldLabels().getOrDefault(key, key), conditionType(scene.fieldType(key)),
                        scene.allowedOperators(key).stream().sorted().toList(),
                        scene.valueLabels().getOrDefault(key, Map.of())));
            }
            var limits = new QuerySceneLimitsRspDTO(DynamicQueryLimits.MAX_DEPTH,
                    DynamicQueryLimits.MAX_NODE_COUNT, DynamicQueryLimits.MAX_IN_SIZE, scene.maxComplexityScore());
            if (result.putIfAbsent(scene.sceneCode(),
                    new QuerySceneMetadataRspDTO(Collections.unmodifiableMap(fields), limits)) != null) {
                throw new IllegalStateException("重复动态查询场景：" + scene.sceneCode());
            }
        }
        metadata = Collections.unmodifiableMap(result);
    }

    public Map<String, QuerySceneMetadataRspDTO> metadata() {
        return metadata;
    }

    private static String conditionType(QueryFieldType kind) {
        return switch (kind) {
            case TEXT -> "text";
            case DATE_TIME -> "dateTime";
            case ENUM -> "enum";
            case NUMBER -> "number";
            case BOOLEAN -> "boolean";
        };
    }
}
