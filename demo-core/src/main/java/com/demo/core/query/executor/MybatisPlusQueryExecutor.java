package com.demo.core.query.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.core.exception.BizException;
import com.demo.core.query.ast.ConditionAstNode;
import com.demo.core.query.ast.ConditionGroupAst;
import com.demo.core.query.ast.ConditionLeafAst;
import com.demo.core.query.ast.QueryAst;
import com.demo.core.query.ast.QueryLogicOperator;
import com.demo.core.query.ast.QueryOperator;
import com.demo.core.query.ast.SortSpec;
import com.demo.core.query.dto.SortItemDTO;
import com.demo.core.query.exception.DynamicQueryErrorCode;
import com.demo.core.query.scene.SceneQueryDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component
public class MybatisPlusQueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(MybatisPlusQueryExecutor.class);

    public <T> Page<T> selectPage(BaseMapper<T> mapper, QueryAst queryAst, SceneQueryDefinition<T> definition) {
        return selectPage(mapper, queryAst, definition, null);
    }

    public <T> Page<T> selectPage(BaseMapper<T> mapper, QueryAst queryAst, SceneQueryDefinition<T> definition,
                                   Consumer<LambdaQueryWrapper<T>> preApply) {
        long startNano = System.nanoTime();
        LambdaQueryWrapper<T> wrapper = buildWrapper(queryAst, definition, preApply);
        Page<T> page = mapper.selectPage(
            new Page<>(defaultIfNull(queryAst.getPageNo(), 1L), defaultIfNull(queryAst.getPageSize(), 20L)),
            wrapper
        );
        logSlowQueryIfNeeded(definition, queryAst, startNano);
        return page;
    }

    public <T> Page<T> selectRange(
            BaseMapper<T> mapper,
            QueryAst queryAst,
            SceneQueryDefinition<T> definition,
            long offset,
            long limit,
            Consumer<LambdaQueryWrapper<T>> preApply
    ) {
        long startNano = System.nanoTime();
        LambdaQueryWrapper<T> countWrapper = buildCountWrapper(queryAst, definition, preApply);
        Long total = mapper.selectCount(countWrapper);

        LambdaQueryWrapper<T> listWrapper = buildWrapper(queryAst, definition, preApply);
        listWrapper.last("LIMIT " + limit + " OFFSET " + offset);
        List<T> records = mapper.selectList(listWrapper);

        Page<T> page = new Page<>(1L, limit, total == null ? 0L : total);
        page.setRecords(records);
        logSlowQueryIfNeeded(definition, queryAst, startNano);
        return page;
    }

    public <T> long selectCount(
            BaseMapper<T> mapper,
            QueryAst queryAst,
            SceneQueryDefinition<T> definition,
            Consumer<LambdaQueryWrapper<T>> preApply
    ) {
        long startNano = System.nanoTime();
        LambdaQueryWrapper<T> wrapper = buildCountWrapper(queryAst, definition, preApply);
        Long total = mapper.selectCount(wrapper);
        logSlowQueryIfNeeded(definition, queryAst, startNano);
        return total == null ? 0L : total;
    }

    public <T> List<T> selectList(BaseMapper<T> mapper, QueryAst queryAst, SceneQueryDefinition<T> definition) {
        long startNano = System.nanoTime();
        LambdaQueryWrapper<T> wrapper = buildWrapper(queryAst, definition);
        List<T> records = mapper.selectList(wrapper);
        logSlowQueryIfNeeded(definition, queryAst, startNano);
        return records;
    }

    public <T> LambdaQueryWrapper<T> buildWrapper(QueryAst queryAst, SceneQueryDefinition<T> definition) {
        return buildWrapper(queryAst, definition, null);
    }

    public <T> LambdaQueryWrapper<T> buildWrapper(QueryAst queryAst, SceneQueryDefinition<T> definition,
                                                   Consumer<LambdaQueryWrapper<T>> preApply) {
        LambdaQueryWrapper<T> wrapper = Wrappers.lambdaQuery();
        if (preApply != null) {
            preApply.accept(wrapper);
        }
        if (queryAst.getRoot() != null) {
            applyNode(wrapper, queryAst.getRoot(), definition);
        }
        applySorts(wrapper, queryAst.getSorts(), definition);
        return wrapper;
    }

    private <T> LambdaQueryWrapper<T> buildCountWrapper(
            QueryAst queryAst,
            SceneQueryDefinition<T> definition,
            Consumer<LambdaQueryWrapper<T>> preApply
    ) {
        LambdaQueryWrapper<T> wrapper = Wrappers.lambdaQuery();
        if (preApply != null) {
            preApply.accept(wrapper);
        }
        if (queryAst.getRoot() != null) {
            applyNode(wrapper, queryAst.getRoot(), definition);
        }
        return wrapper;
    }

    private <T> void applyNode(LambdaQueryWrapper<T> wrapper, ConditionAstNode node, SceneQueryDefinition<T> definition) {
        if (node instanceof ConditionLeafAst leaf) {
            applyLeaf(wrapper, leaf, definition);
            return;
        }
        ConditionGroupAst group = (ConditionGroupAst) node;
        if (group.getChildren() == null || group.getChildren().isEmpty()) {
            return;
        }
        applyGroupContents(wrapper, group, definition);
    }

    private <T> void applyGroupContents(
        LambdaQueryWrapper<T> wrapper,
        ConditionGroupAst group,
        SceneQueryDefinition<T> definition
    ) {
        for (int index = 0; index < group.getChildren().size(); index++) {
            ConditionAstNode child = group.getChildren().get(index);
            if (index == 0) {
                applyNested(wrapper, child, definition);
                continue;
            }
            if (group.getLogic() == QueryLogicOperator.OR) {
                wrapper.or(nested -> applyNested(nested, child, definition));
            } else {
                wrapper.and(nested -> applyNested(nested, child, definition));
            }
        }
    }

    private <T> void applyNested(LambdaQueryWrapper<T> wrapper, ConditionAstNode child, SceneQueryDefinition<T> definition) {
        if (child instanceof ConditionLeafAst leaf) {
            applyLeaf(wrapper, leaf, definition);
            return;
        }
        wrapper.nested(nested -> applyGroupContents(nested, (ConditionGroupAst) child, definition));
    }

    private <T> void applyLeaf(LambdaQueryWrapper<T> wrapper, ConditionLeafAst leaf, SceneQueryDefinition<T> definition) {
        String fieldKey = leaf.getFieldKey();
        QueryOperator operator = leaf.getOperator();
        Set<QueryOperator> allowedOperators = definition.allowedOperators(fieldKey);
        if (!allowedOperators.contains(operator)) {
            throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_UNSUPPORTED_OPERATOR);
        }
        BiConsumer<LambdaQueryWrapper<T>, ConditionLeafAst> customLeafApplier =
                definition.customLeafAppliers().get(fieldKey);
        if (customLeafApplier != null) {
            customLeafApplier.accept(wrapper, leaf);
            return;
        }
        if (definition.textFields().containsKey(fieldKey)) {
            applyTextLeaf(wrapper, definition.textFields().get(fieldKey), operator, leaf.getTypedValue());
            return;
        }
        if (definition.dateTimeFields().containsKey(fieldKey)) {
            applyDateTimeLeaf(wrapper, definition.dateTimeFields().get(fieldKey), operator, leaf.getTypedValue());
            return;
        }
        if (definition.enumFields().containsKey(fieldKey)) {
            applyEnumLeaf(wrapper, definition.enumFields().get(fieldKey), operator, leaf.getTypedValue());
            return;
        }
        throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_UNSUPPORTED_NODE);
    }

    private <T> void applyTextLeaf(
        LambdaQueryWrapper<T> wrapper,
        SFunction<T, String> field,
        QueryOperator operator,
        Object typedValue
    ) {
        switch (operator) {
            case EQ -> wrapper.eq(field, typedValue);
            case CONTAINS -> wrapper.like(field, typedValue);
            case STARTS_WITH -> wrapper.likeRight(field, typedValue);
            case ENDS_WITH -> wrapper.likeLeft(field, typedValue);
            case IN -> wrapper.in(field, castList(typedValue));
            case IS_NULL -> wrapper.isNull(field);
            case IS_NOT_NULL -> wrapper.isNotNull(field);
            default -> throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_UNSUPPORTED_OPERATOR);
        }
    }

    private <T> void applyDateTimeLeaf(
        LambdaQueryWrapper<T> wrapper,
        SFunction<T, LocalDateTime> field,
        QueryOperator operator,
        Object typedValue
    ) {
        switch (operator) {
            case GT -> wrapper.gt(field, typedValue);
            case GTE -> wrapper.ge(field, typedValue);
            case LT -> wrapper.lt(field, typedValue);
            case LTE -> wrapper.le(field, typedValue);
            case BETWEEN -> {
                List<?> values = castList(typedValue);
                wrapper.between(field, values.get(0), values.get(1));
            }
            case IS_NULL -> wrapper.isNull(field);
            case IS_NOT_NULL -> wrapper.isNotNull(field);
            default -> throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_UNSUPPORTED_OPERATOR);
        }
    }

    private <T> void applyEnumLeaf(
        LambdaQueryWrapper<T> wrapper,
        SFunction<T, ?> field,
        QueryOperator operator,
        Object typedValue
    ) {
        switch (operator) {
            case EQ -> wrapper.eq(field, typedValue);
            case IN -> wrapper.in(field, castList(typedValue));
            case IS_NULL -> wrapper.isNull(field);
            case IS_NOT_NULL -> wrapper.isNotNull(field);
            default -> throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_UNSUPPORTED_OPERATOR);
        }
    }

    private <T> void applySorts(
        LambdaQueryWrapper<T> wrapper,
        List<SortSpec> explicitSorts,
        SceneQueryDefinition<T> definition
    ) {
        List<SortSpec> sorts = (explicitSorts == null || explicitSorts.isEmpty())
            ? definition.defaultSorts()
            : explicitSorts;
        if (sorts == null || sorts.isEmpty()) {
            return;
        }
        Map<String, SFunction<T, ?>> sortFields = definition.sortFields();
        for (SortSpec sort : sorts) {
            SFunction<T, ?> field = sortFields.get(sort.getFieldKey());
            if (field == null) {
                throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_INVALID_SORT_FIELD);
            }
            wrapper.orderBy(
                true,
                sort.getDirection() == SortItemDTO.SortDirection.ASC,
                field
            );
        }
    }

    @SuppressWarnings("unchecked")
    private List<?> castList(Object typedValue) {
        return (List<?>) typedValue;
    }

    private long defaultIfNull(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private void logSlowQueryIfNeeded(SceneQueryDefinition<?> definition, QueryAst queryAst, long startNano) {
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
        if (elapsedMs >= definition.slowQueryWarnThresholdMs()) {
            log.warn(
                "dynamic query slow scene={} elapsedMs={} pageNo={} pageSize={}",
                definition.sceneCode(),
                elapsedMs,
                queryAst.getPageNo(),
                queryAst.getPageSize()
            );
        }
    }
}
