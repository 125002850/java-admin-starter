package com.oigit.admin.core.query.scene;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.*;
import com.oigit.admin.core.query.dto.*;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TypedQueryTests {
    enum Field { amount, active }
    static final SceneQueryDefinition<Row> DEFINITION = new SceneQueryDefinition<>() {
        public String sceneCode() { return "test.typed"; }
        public Map<String, SFunction<Row, String>> textFields() { return Map.of(); }
        public Map<String, SFunction<Row, LocalDateTime>> dateTimeFields() { return Map.of(); }
        public Map<String, SFunction<Row, ?>> enumFields() { return Map.of(); }
        public Map<String, SFunction<Row, Boolean>> booleanFields() { return Map.of("active", Row::getActive); }
        public Map<String, SFunction<Row, ? extends Number>> numberFields() { return Map.of("amount", Row::getAmount); }
        public Map<String, SFunction<Row, ?>> sortFields() { return Map.of("id", Row::getId, "createTime", Row::getCreateTime); }
    };

    @Test
    void mapsNestedTypedConditionsAndBindsValuesWithStableSort() {
        var assistant = new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "typed-query");
        assistant.setCurrentNamespace(Row.class.getName());
        TableInfoHelper.initTableInfo(assistant, Row.class);
        var number = new NumberConditionDTO<Field>();
        number.setField(Field.amount);
        number.setOp(NumberConditionDTO.NumberOperator.BETWEEN);
        number.setStart(new BigDecimal("1.25"));
        number.setEnd(new BigDecimal("9.75"));
        var bool = new BooleanConditionDTO<Field>();
        bool.setField(Field.active);
        bool.setOp(BooleanConditionDTO.BooleanOperator.EQ);
        bool.setValue(false);
        var group = new ConditionGroupDTO<AbstractConditionNodeDTO>();
        group.setLogic(ConditionGroupDTO.LogicOperator.OR);
        group.setChildren(List.of(number, bool));
        QueryAst ast = DynamicQueryAstMapper.toQueryAst(group, List.of());
        var wrapper = new MybatisPlusQueryExecutor().buildWrapper(ast, DEFINITION);
        assertThat(wrapper.getSqlSegment()).contains("amount BETWEEN", " OR ", "active =", "ORDER BY create_time DESC,id DESC");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(new BigDecimal("1.25"), new BigDecimal("9.75"), false);
        ast.setSorts(List.of(new SortSpec("createTime", SortItemDTO.SortDirection.ASC), new SortSpec("id", SortItemDTO.SortDirection.ASC)));
        assertThat(new MybatisPlusQueryExecutor().buildWrapper(ast, DEFINITION).getSqlSegment())
                .contains("ORDER BY create_time ASC,id ASC").doesNotContain("id DESC");
        ((ConditionLeafAst)((ConditionGroupAst)ast.getRoot()).getChildren().get(0)).setFieldKey("unknown");
        assertThatThrownBy(() -> new MybatisPlusQueryExecutor().buildWrapper(ast, DEFINITION)).isInstanceOf(BizException.class);
    }

    @Test
    void rejectsMissingOrReversedNumericBoundsAndAcceptsFalse() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var number = new NumberConditionDTO<Field>();
            number.setField(Field.amount);
            number.setOp(NumberConditionDTO.NumberOperator.BETWEEN);
            assertThat(validator.validate(number)).isNotEmpty();
            number.setStart(BigDecimal.TEN);
            number.setEnd(BigDecimal.ONE);
            assertThat(validator.validate(number)).isNotEmpty();
            var bool = new BooleanConditionDTO<Field>();
            bool.setField(Field.active);
            bool.setOp(BooleanConditionDTO.BooleanOperator.EQ);
            assertThat(validator.validate(bool)).isNotEmpty();
            bool.setValue(false);
            assertThat(validator.validate(bool)).isEmpty();
        }
    }

    static class Row {
        @TableId private Long id;
        private BigDecimal amount;
        private Boolean active;
        private LocalDateTime createTime;
        public Long getId() { return id; }
        public BigDecimal getAmount() { return amount; }
        public Boolean getActive() { return active; }
        public LocalDateTime getCreateTime() { return createTime; }
    }
}
