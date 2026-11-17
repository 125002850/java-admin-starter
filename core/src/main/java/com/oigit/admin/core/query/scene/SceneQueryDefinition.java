package com.oigit.admin.core.query.scene;

import com.oigit.admin.core.query.ast.SortSpec;

import java.util.List;
import java.util.Map;

public interface SceneQueryDefinition<T> {

    String sceneCode();

    Map<String, QueryFieldSpec<T>> fields();

    default Map<String, String> fieldLabels() {
        return fields().entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> entry.getValue().label()));
    }

    default Map<String, Map<String, String>> valueLabels() {
        return fields().entrySet().stream()
                .filter(entry -> !entry.getValue().valueLabels().isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> entry.getValue().valueLabels()));
    }

    default List<SortSpec> defaultSorts() {
        return SceneQuerySorts.defaultCreationSorts(fields().entrySet().stream()
                .filter(entry -> entry.getValue().isSortable()).map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet()));
    }

    default int maxComplexityScore() {
        return 20;
    }

    default int maxExportRows() {
        return 5000;
    }

    default long slowQueryWarnThresholdMs() {
        return 300L;
    }
}
