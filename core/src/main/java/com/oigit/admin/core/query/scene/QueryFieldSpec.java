package com.oigit.admin.core.query.scene;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.oigit.admin.core.query.ast.ConditionLeafAst;
import com.oigit.admin.core.query.ast.QueryOperator;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/** 单个 DSL 字段的完整元数据，是过滤、排序、展示和文档生成的唯一来源。 */
public final class QueryFieldSpec<T> {

    private static final QueryOperator[] TEXT_OPERATORS = {
            QueryOperator.EQ, QueryOperator.NE, QueryOperator.CONTAINS, QueryOperator.NOT_CONTAINS, QueryOperator.STARTS_WITH,
            QueryOperator.ENDS_WITH, QueryOperator.IN, QueryOperator.NOT_IN, QueryOperator.IS_NULL, QueryOperator.IS_NOT_NULL
    };
    private static final QueryOperator[] DATE_TIME_OPERATORS = {
            QueryOperator.EQ, QueryOperator.NE, QueryOperator.GT, QueryOperator.GTE, QueryOperator.LT, QueryOperator.LTE,
            QueryOperator.BETWEEN, QueryOperator.IS_NULL, QueryOperator.IS_NOT_NULL
    };
    private static final QueryOperator[] ENUM_OPERATORS = {
            QueryOperator.EQ, QueryOperator.NE, QueryOperator.IN, QueryOperator.NOT_IN, QueryOperator.IS_NULL, QueryOperator.IS_NOT_NULL
    };

    public enum Kind {
        TEXT,
        DATE_TIME,
        ENUM,
        NUMBER,
        BOOLEAN,
        SORT_ONLY
    }

    private final Kind kind;
    private final SFunction<T, ?> column;
    private final BiConsumer<LambdaQueryWrapper<T>, ConditionLeafAst> customLeafApplier;
    private final Set<QueryOperator> allowedOperators;
    private final String label;
    private final Map<String, String> valueLabels;
    private final SFunction<T, ?> sortColumn;

    private QueryFieldSpec(
            Kind kind,
            SFunction<T, ?> column,
            BiConsumer<LambdaQueryWrapper<T>, ConditionLeafAst> customLeafApplier,
            Set<QueryOperator> allowedOperators,
            String label,
            Map<String, String> valueLabels,
            SFunction<T, ?> sortColumn
    ) {
        this.kind = Objects.requireNonNull(kind);
        this.column = column;
        this.customLeafApplier = customLeafApplier;
        this.allowedOperators = Set.copyOf(allowedOperators);
        this.label = Objects.requireNonNull(label);
        this.valueLabels = Map.copyOf(valueLabels);
        this.sortColumn = sortColumn;
    }

    public static <T> QueryFieldSpec<T> text(
            SFunction<T, String> column, String label, QueryOperator... operators
    ) {
        return filter(Kind.TEXT, column, null, label, operators);
    }

    public static <T> QueryFieldSpec<T> text(SFunction<T, String> column, String label) {
        return text(column, label, TEXT_OPERATORS);
    }

    public static <T> QueryFieldSpec<T> dateTime(
            SFunction<T, ?> column, String label, QueryOperator... operators
    ) {
        return filter(Kind.DATE_TIME, column, null, label, operators);
    }

    public static <T> QueryFieldSpec<T> dateTime(SFunction<T, ?> column, String label) {
        return dateTime(column, label, DATE_TIME_OPERATORS);
    }

    public static <T> QueryFieldSpec<T> enumeration(
            SFunction<T, ?> column, String label, QueryOperator... operators
    ) {
        return filter(Kind.ENUM, column, null, label, operators);
    }

    public static <T> QueryFieldSpec<T> enumeration(SFunction<T, ?> column, String label) {
        return enumeration(column, label, ENUM_OPERATORS);
    }

    public static <T> QueryFieldSpec<T> number(SFunction<T, ? extends Number> column, String label) {
        return filter(Kind.NUMBER, column, null, label, DATE_TIME_OPERATORS);
    }

    public static <T> QueryFieldSpec<T> bool(SFunction<T, Boolean> column, String label) {
        return filter(Kind.BOOLEAN, column, null, label, QueryOperator.EQ, QueryOperator.NE,
                QueryOperator.IS_NULL, QueryOperator.IS_NOT_NULL);
    }

    public static <T> QueryFieldSpec<T> customText(
            BiConsumer<LambdaQueryWrapper<T>, ConditionLeafAst> applier,
            String label,
            QueryOperator... operators
    ) {
        return filter(Kind.TEXT, null, Objects.requireNonNull(applier), label, operators);
    }

    public static <T> QueryFieldSpec<T> sortOnly(SFunction<T, ?> column, String label) {
        return new QueryFieldSpec<>(Kind.SORT_ONLY, null, null, Set.of(), label, Map.of(), column);
    }

    private static <T> QueryFieldSpec<T> filter(
            Kind kind,
            SFunction<T, ?> column,
            BiConsumer<LambdaQueryWrapper<T>, ConditionLeafAst> applier,
            String label,
            QueryOperator... operators
    ) {
        if (column == null && applier == null) {
            throw new IllegalArgumentException("查询字段必须配置列或自定义处理器");
        }
        return new QueryFieldSpec<>(kind, column, applier, Set.copyOf(Arrays.asList(operators)), label, Map.of(), null);
    }

    public QueryFieldSpec<T> sortable() {
        if (column == null) {
            throw new IllegalStateException("自定义字段必须通过 sortableBy 指定排序列");
        }
        return sortableBy(column);
    }

    public QueryFieldSpec<T> sortableBy(SFunction<T, ?> field) {
        return new QueryFieldSpec<>(kind, column, customLeafApplier, allowedOperators, label, valueLabels, field);
    }

    public QueryFieldSpec<T> withValueLabels(Map<String, String> labels) {
        return new QueryFieldSpec<>(kind, column, customLeafApplier, allowedOperators, label, labels, sortColumn);
    }

    public Kind kind() { return kind; }
    public SFunction<T, ?> column() { return column; }
    public BiConsumer<LambdaQueryWrapper<T>, ConditionLeafAst> customLeafApplier() { return customLeafApplier; }
    public Set<QueryOperator> allowedOperators() { return allowedOperators; }
    public String label() { return label; }
    public Map<String, String> valueLabels() { return valueLabels; }
    public SFunction<T, ?> sortColumn() { return sortColumn; }
    public boolean filterable() { return kind != Kind.SORT_ONLY; }
    public boolean isSortable() { return sortColumn != null; }
}
