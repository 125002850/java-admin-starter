package com.oigit.admin.core.query.scene;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.*;
import com.oigit.admin.core.query.dto.*;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class QueryFieldSpecTests {
    enum Field { name, status, created }
    private final ObjectMapper json = new ObjectMapper();
    private final MybatisPlusQueryExecutor executor = new MybatisPlusQueryExecutor();
    private final SceneQueryDefinition<Row> scene = new SceneQueryDefinition<>() {
        public String sceneCode() { return "test.fields"; }
        public Map<String, QueryFieldSpec<Row>> fields() {
            return Map.of("name", QueryFieldSpec.text(Row::getName, "名称"),
                    "status", QueryFieldSpec.enumeration(Row::getStatus, "状态"),
                    "created", QueryFieldSpec.dateTime(Row::getCreated, "创建时间"),
                    "owner", QueryFieldSpec.enumeration(Row::getId, "所有者", QueryOperator.EQ),
                    "id", QueryFieldSpec.sortOnly(Row::getId, "ID"));
        }
    };
    @BeforeAll static void tableMetadata() {
        var assistant = new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "query-field-spec");
        assistant.setCurrentNamespace(Row.class.getName());
        TableInfoHelper.initTableInfo(assistant, Row.class);
    }
    @Test void typedTextNegationReachesSqlWithoutInterpolatingValues() throws Exception {
        for (String operator : List.of("NE", "NOT_IN", "NOT_CONTAINS")) {
            String payload = "NOT_IN".equals(operator) ? "\"values\":[\"x' OR 1=1 --\"]" : "\"value\":\"x' OR 1=1 --\"";
            var condition = json.readValue("{\"field\":\"name\",\"op\":\""+operator+"\","+payload+"}",
                    new TypeReference<TextConditionDTO<Field>>() {});
            assertThat(condition.isPayloadValid()).isTrue();
            var wrapper = executor.buildWrapper(DynamicQueryAstMapper.toQueryAst(condition, List.of()), scene);
            assertThat(wrapper.getSqlSegment()).contains(switch (operator) {
                case "NE" -> "<>"; case "NOT_IN" -> "NOT IN"; default -> "NOT LIKE";
            }).doesNotContain("OR 1=1");
            assertThat(wrapper.getParamNameValuePairs().values()).anySatisfy(value ->
                    assertThat(String.valueOf(value)).contains("x' OR 1=1 --"));
        }
    }
    @Test void typedEnumAndTimeNegationKeepTheirPayloadTypes() throws Exception {
        var enumeration = json.readValue("{\"field\":\"status\",\"op\":\"NOT_IN\",\"values\":[1,2]}",
                new TypeReference<EnumConditionDTO<Field,Integer>>() {});
        var wrapper = executor.buildWrapper(DynamicQueryAstMapper.toQueryAst(enumeration, List.of()), scene);
        assertThat(wrapper.getSqlSegment()).contains("NOT IN");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1,2);
        var date = json.readValue("{\"field\":\"created\",\"op\":\"NE\",\"value\":\"2026-11-17 12:00:00\"}",
                new TypeReference<DateTimeConditionDTO<Field>>() {});
        wrapper = executor.buildWrapper(DynamicQueryAstMapper.toQueryAst(date, List.of()), scene);
        assertThat(wrapper.getSqlSegment()).contains("<>");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(LocalDateTime.of(2026,11,17,12,0));
    }
    @Test void unknownSortOnlyAndRestrictedFieldsStayClosed() {
        for (String field : List.of("missing", "id", "owner")) {
            var leaf = new ConditionLeafAst(); leaf.setFieldKey(field); leaf.setOperator(QueryOperator.NOT_IN);
            leaf.setTypedValue(List.of(1)); var ast = new QueryAst(); ast.setRoot(leaf);
            assertThatThrownBy(() -> executor.buildWrapper(ast, scene)).isInstanceOf(BizException.class);
        }
    }
    static class Row {
        @TableId private Long id;
        private String name;
        private Integer status;
        private LocalDateTime created;
        public Long getId() { return id; }
        public String getName() { return name; }
        public Integer getStatus() { return status; }
        public LocalDateTime getCreated() { return created; }
    }
}
