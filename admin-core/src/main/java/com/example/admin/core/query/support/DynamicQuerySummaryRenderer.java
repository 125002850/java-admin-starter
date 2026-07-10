package com.example.admin.core.query.support;

import com.example.admin.core.query.ast.ConditionAstNode;
import com.example.admin.core.query.ast.ConditionGroupAst;
import com.example.admin.core.query.ast.ConditionLeafAst;
import com.example.admin.core.query.ast.QueryAst;
import com.example.admin.core.query.ast.QueryLogicOperator;
import com.example.admin.core.query.ast.QueryOperator;
import com.example.admin.core.query.scene.SceneQueryDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DynamicQuerySummaryRenderer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String render(QueryAst queryAst, SceneQueryDefinition<?> definition) {
        if (queryAst == null || queryAst.getRoot() == null || definition == null) {
            return "";
        }
        return renderNode(queryAst.getRoot(), definition, true);
    }

    private String renderNode(ConditionAstNode node, SceneQueryDefinition<?> definition, boolean root) {
        if (node instanceof ConditionLeafAst leaf) {
            return renderLeaf(leaf, definition);
        }
        if (node instanceof ConditionGroupAst group) {
            return renderGroup(group, definition, root);
        }
        return "";
    }

    private String renderGroup(ConditionGroupAst group, SceneQueryDefinition<?> definition, boolean root) {
        List<ConditionAstNode> children = group.getChildren();
        if (children == null || children.isEmpty()) {
            return "";
        }
        String separator = group.getLogic() == QueryLogicOperator.OR ? " 或 " : " 且 ";
        String summary = children.stream()
                .map(child -> renderNode(child, definition, false))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(separator));
        if (!StringUtils.hasText(summary)) {
            return "";
        }
        return root || children.size() == 1 ? summary : "(" + summary + ")";
    }

    private String renderLeaf(ConditionLeafAst leaf, SceneQueryDefinition<?> definition) {
        String fieldLabel = fieldLabel(leaf.getFieldKey(), definition);
        QueryOperator operator = leaf.getOperator();
        if (operator == QueryOperator.IS_NULL || operator == QueryOperator.IS_NOT_NULL) {
            return fieldLabel + " " + operatorLabel(operator);
        }
        return fieldLabel + " " + operatorLabel(operator) + " " + valueText(leaf, definition);
    }

    private String fieldLabel(String fieldKey, SceneQueryDefinition<?> definition) {
        return definition.fieldLabels().getOrDefault(fieldKey, fieldKey);
    }

    private String operatorLabel(QueryOperator operator) {
        return switch (operator) {
            case EQ -> "等于";
            case CONTAINS -> "包含";
            case STARTS_WITH -> "开头为";
            case ENDS_WITH -> "结尾为";
            case IN -> "属于";
            case IS_NULL -> "为空";
            case IS_NOT_NULL -> "不为空";
            case GT -> "大于";
            case GTE -> "大于等于";
            case LT -> "小于";
            case LTE -> "小于等于";
            case BETWEEN -> "介于";
        };
    }

    private String valueText(ConditionLeafAst leaf, SceneQueryDefinition<?> definition) {
        Object value = leaf.getTypedValue();
        if (leaf.getOperator() == QueryOperator.BETWEEN && value instanceof List<?> values && values.size() >= 2) {
            return singleValueText(leaf.getFieldKey(), values.get(0), definition)
                    + " 至 "
                    + singleValueText(leaf.getFieldKey(), values.get(1), definition);
        }
        if (value instanceof List<?> values) {
            return values.stream()
                    .map(item -> singleValueText(leaf.getFieldKey(), item, definition))
                    .collect(Collectors.joining("、"));
        }
        return singleValueText(leaf.getFieldKey(), value, definition);
    }

    private String singleValueText(String fieldKey, Object value, SceneQueryDefinition<?> definition) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dateTime) {
            return DATE_TIME_FORMATTER.format(dateTime);
        }
        String rawValue = String.valueOf(value);
        Map<String, String> labels = definition.valueLabels().get(fieldKey);
        if (labels == null) {
            return rawValue;
        }
        return labels.getOrDefault(rawValue, rawValue);
    }
}
