package com.example.admin.core.query.scene;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.example.admin.core.query.ast.ConditionLeafAst;
import com.example.admin.core.query.ast.QueryOperator;
import com.example.admin.core.query.ast.SortSpec;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public interface SceneQueryDefinition<T> {

    String sceneCode();

    Map<String, SFunction<T, String>> textFields();

    Map<String, SFunction<T, LocalDateTime>> dateTimeFields();

    Map<String, SFunction<T, ?>> enumFields();

    default Map<String, BiConsumer<LambdaQueryWrapper<T>, ConditionLeafAst>> customLeafAppliers() {
        return Map.of();
    }

    Map<String, SFunction<T, ?>> sortFields();

    default Map<String, String> fieldLabels() {
        return Map.of();
    }

    default Map<String, Map<String, String>> valueLabels() {
        return Map.of();
    }

    default Set<QueryOperator> allowedOperators(String fieldKey) {
        return Set.of(QueryOperator.values());
    }

    default List<SortSpec> defaultSorts() {
        return List.of();
    }

    default int maxComplexityScore() {
        return 100;
    }

    default int maxExportRows() {
        return 5000;
    }

    default long slowQueryWarnThresholdMs() {
        return 300L;
    }
}
