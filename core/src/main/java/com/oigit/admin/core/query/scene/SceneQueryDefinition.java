package com.oigit.admin.core.query.scene;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.oigit.admin.core.query.ast.ConditionLeafAst;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.ast.SortSpec;

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

    default Map<String, SFunction<T, Boolean>> booleanFields() {
        return Map.of();
    }

    default Map<String, SFunction<T, ? extends Number>> numberFields() {
        return Map.of();
    }

    default Map<String, QueryFieldType> customFieldTypes() {
        return Map.of();
    }

    default QueryFieldType fieldType(String fieldKey) {
        if (textFields().containsKey(fieldKey)) return QueryFieldType.TEXT;
        if (dateTimeFields().containsKey(fieldKey)) return QueryFieldType.DATE_TIME;
        if (enumFields().containsKey(fieldKey)) return QueryFieldType.ENUM;
        if (booleanFields().containsKey(fieldKey)) return QueryFieldType.BOOLEAN;
        if (numberFields().containsKey(fieldKey)) return QueryFieldType.NUMBER;
        return customFieldTypes().get(fieldKey);
    }

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
        QueryFieldType type = fieldType(fieldKey);
        if (type == null) return Set.of(QueryOperator.values());
        return switch (type) {
            case TEXT -> Set.of(
                    QueryOperator.EQ, QueryOperator.NE,
                    QueryOperator.CONTAINS, QueryOperator.NOT_CONTAINS,
                    QueryOperator.STARTS_WITH, QueryOperator.ENDS_WITH,
                    QueryOperator.IN, QueryOperator.NOT_IN,
                    QueryOperator.IS_NULL, QueryOperator.IS_NOT_NULL
            );
            case ENUM -> Set.of(
                    QueryOperator.EQ, QueryOperator.NE,
                    QueryOperator.IN, QueryOperator.NOT_IN,
                    QueryOperator.IS_NULL, QueryOperator.IS_NOT_NULL
            );
            case BOOLEAN -> Set.of(
                    QueryOperator.EQ, QueryOperator.NE,
                    QueryOperator.IS_NULL, QueryOperator.IS_NOT_NULL
            );
            case DATE_TIME, NUMBER -> Set.of(
                    QueryOperator.EQ, QueryOperator.NE,
                    QueryOperator.GT, QueryOperator.GTE,
                    QueryOperator.LT, QueryOperator.LTE,
                    QueryOperator.BETWEEN,
                    QueryOperator.IS_NULL, QueryOperator.IS_NOT_NULL
            );
        };
    }

    default List<SortSpec> defaultSorts() {
        return SceneQuerySorts.defaultCreationSorts(sortFields().keySet());
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
