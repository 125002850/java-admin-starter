package com.oigit.admin.queryscheme.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.scene.QueryFieldType;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.core.query.validation.DynamicQueryLimits;
import com.oigit.admin.queryscheme.enums.QuerySchemeErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QuerySchemeContentValidator {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_CONTENT_LENGTH = 65_535;

    private final ObjectMapper objectMapper;
    private final Map<String, SceneQueryDefinition<?>> definitions;

    public QuerySchemeContentValidator(
            ObjectMapper objectMapper,
            List<SceneQueryDefinition<?>> definitions
    ) {
        this.objectMapper = objectMapper;
        Map<String, SceneQueryDefinition<?>> indexed = new HashMap<>();
        definitions.forEach(definition -> {
            if (indexed.put(definition.sceneCode(), definition) != null) {
                throw new IllegalStateException("duplicate dynamic query scene: " + definition.sceneCode());
            }
        });
        this.definitions = Map.copyOf(indexed);
    }

    public void validateScene(String sceneCode) {
        if (!definitions.containsKey(sceneCode)) {
            throw new BizException(QuerySchemeErrorCode.SCENE_NOT_FOUND);
        }
    }

    public void validate(String sceneCode, String contentJson) {
        SceneQueryDefinition<?> definition = definitions.get(sceneCode);
        if (definition == null) throw new BizException(QuerySchemeErrorCode.SCENE_NOT_FOUND);
        if (contentJson == null || contentJson.length() > MAX_CONTENT_LENGTH) invalid();

        try {
            JsonNode content = objectMapper.readTree(contentJson);
            if (content == null || !content.isObject() || content.path("schemaVersion").asInt(-1) != SCHEMA_VERSION) invalid();

            ValidationStats total = new ValidationStats();
            JsonNode quickConditions = content.path("quickConditions");
            if (!quickConditions.isArray()) invalid();
            if (quickConditions.isArray()) {
                for (JsonNode quick : quickConditions) {
                    if (!quick.isObject() || quick.path("columnId").asText("").isBlank()) invalid();
                    total.add(validateNode(quick.get("condition"), definition, 1));
                }
            }
            JsonNode advanced = content.get("advancedCondition");
            if (advanced != null && !advanced.isNull()) {
                total.add(validateNode(advanced, definition, 1));
            }
            int roots = quickConditions.size() + (advanced != null && !advanced.isNull() ? 1 : 0);
            if (roots > 1) { total.nodes++; total.depth++; }
            if (total.nodes > DynamicQueryLimits.MAX_NODE_COUNT
                    || total.depth > DynamicQueryLimits.MAX_DEPTH
                    || total.complexity > definition.maxComplexityScore()) {
                invalid();
            }
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            invalid();
        }
    }

    private ValidationStats validateNode(JsonNode node, SceneQueryDefinition<?> definition, int depth) {
        if (depth > DynamicQueryLimits.MAX_DEPTH || node == null || !node.isObject()) invalid();
        String nodeType = node.path("nodeType").asText("");
        if ("compose".equals(nodeType)) {
            String logic = node.path("logic").asText("");
            JsonNode children = node.path("children");
            if (!("AND".equals(logic) || "OR".equals(logic)) || !children.isArray() || children.isEmpty()) {
                invalid();
            }
            ValidationStats stats = new ValidationStats(1, depth, "OR".equals(logic) ? 2 : 0);
            for (JsonNode child : children) stats.add(validateNode(child, definition, depth + 1));
            return stats;
        }

        QueryFieldType expectedType = switch (nodeType) {
            case "text" -> QueryFieldType.TEXT;
            case "enum" -> QueryFieldType.ENUM;
            case "boolean" -> QueryFieldType.BOOLEAN;
            case "dateTime" -> QueryFieldType.DATE_TIME;
            case "number" -> QueryFieldType.NUMBER;
            default -> throw invalidException();
        };
        String field = node.path("field").asText("");
        if (field.isBlank() || definition.fieldType(field) != expectedType) invalid();

        QueryOperator operator;
        try {
            operator = QueryOperator.valueOf(node.path("op").asText(""));
        } catch (IllegalArgumentException ex) {
            throw invalidException();
        }
        if (!definition.allowedOperators(field).contains(operator)) invalid();
        validatePayload(node, expectedType, operator);

        int complexity = 1;
        if (operator == QueryOperator.CONTAINS
                || operator == QueryOperator.NOT_CONTAINS
                || operator == QueryOperator.ENDS_WITH) complexity += 2;
        if (operator == QueryOperator.BETWEEN) complexity += 1;
        if (operator == QueryOperator.IN || operator == QueryOperator.NOT_IN) {
            complexity += 1 + node.path("values").size() / 20;
        }
        return new ValidationStats(1, depth, complexity);
    }

    private void validatePayload(JsonNode node, QueryFieldType type, QueryOperator operator) {
        if (operator == QueryOperator.IS_NULL || operator == QueryOperator.IS_NOT_NULL) return;
        if (operator == QueryOperator.IN || operator == QueryOperator.NOT_IN) {
            JsonNode values = node.path("values");
            if (!values.isArray() || values.isEmpty() || values.size() > DynamicQueryLimits.MAX_IN_SIZE) invalid();
            values.forEach(value -> validateScalar(value, type));
            return;
        }
        if (operator == QueryOperator.BETWEEN) {
            validateScalar(node.get("start"), type);
            validateScalar(node.get("end"), type);
            if (type == QueryFieldType.NUMBER && node.get("start").decimalValue().compareTo(node.get("end").decimalValue()) > 0) invalid();
            if (type == QueryFieldType.DATE_TIME && parseDate(node.get("start").asText()).isAfter(parseDate(node.get("end").asText()))) invalid();
            return;
        }
        validateScalar(node.get("value"), type);
    }

    private void validateScalar(JsonNode value, QueryFieldType type) {
        if (value == null || value.isNull()) invalid();
        boolean valid = switch (type) {
            case TEXT -> value.isTextual() && !value.asText().isBlank();
            case DATE_TIME -> value.isTextual() && validDate(value.asText());
            case ENUM -> value.isValueNode() && !value.isContainerNode();
            case BOOLEAN -> value.isBoolean();
            case NUMBER -> value.isNumber();
        };
        if (!valid) invalid();
    }

    private java.time.LocalDateTime parseDate(String value) {
        return java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private boolean validDate(String value) {
        try { parseDate(value); return true; }
        catch (java.time.format.DateTimeParseException ex) { return false; }
    }

    private void invalid() {
        throw invalidException();
    }

    private BizException invalidException() {
        return new BizException(QuerySchemeErrorCode.CONTENT_INVALID);
    }

    private static final class ValidationStats {
        private int nodes;
        private int depth;
        private int complexity;

        private ValidationStats() { }
        private ValidationStats(int nodes, int depth, int complexity) {
            this.nodes = nodes;
            this.depth = depth;
            this.complexity = complexity;
        }
        private void add(ValidationStats other) {
            nodes += other.nodes;
            depth = Math.max(depth, other.depth);
            complexity += other.complexity;
        }
    }
}
