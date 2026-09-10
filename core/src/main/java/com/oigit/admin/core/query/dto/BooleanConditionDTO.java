package com.oigit.admin.core.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "布尔字段查询条件")
public class BooleanConditionDTO<F extends Enum<F>> implements AbstractConditionNodeDTO {

    public enum BooleanOperator {
        EQ,
        NE,
        IS_NULL,
        IS_NOT_NULL
    }

    @NotNull
    @Schema(description = "布尔字段")
    private F field;

    @NotNull
    @Schema(description = "布尔操作符")
    private BooleanOperator op;

    @Schema(description = "EQ/NE 操作符使用")
    private Boolean value;

    @AssertTrue(message = "布尔条件 value 不匹配操作符")
    public boolean isPayloadValid() {
        if (op == null || op == BooleanOperator.IS_NULL || op == BooleanOperator.IS_NOT_NULL) {
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

    public BooleanOperator getOp() {
        return op;
    }

    public void setOp(BooleanOperator op) {
        this.op = op;
    }

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }
}
