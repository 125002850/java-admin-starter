package com.demo.core.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "枚举码值查询条件")
public class EnumConditionDTO<F extends Enum<F>, V> implements AbstractConditionNodeDTO {

    public enum EnumOperator {
        EQ,
        IN,
        IS_NULL,
        IS_NOT_NULL
    }

    @NotNull
    @Schema(description = "枚举字段")
    private F field;

    @NotNull
    @Schema(description = "枚举操作符")
    private EnumOperator op;

    @Schema(description = "单值操作符使用")
    private V value;

    @Schema(description = "IN 操作符使用")
    private List<V> values;

    @AssertTrue(message = "枚举条件 value/values 不匹配操作符")
    public boolean isPayloadValid() {
        if (op == null) {
            return true;
        }
        if (op == EnumOperator.IN) {
            return values != null
                && !values.isEmpty()
                && values.stream().allMatch(item -> item != null);
        }
        if (op == EnumOperator.IS_NULL || op == EnumOperator.IS_NOT_NULL) {
            return true;
        }
        return value != null;
    }

    public F getField() {
        return field;
    }

    public void setField(F field) {
        this.field = field;
    }

    public EnumOperator getOp() {
        return op;
    }

    public void setOp(EnumOperator op) {
        this.op = op;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public List<V> getValues() {
        return values;
    }

    public void setValues(List<V> values) {
        this.values = values;
    }
}
