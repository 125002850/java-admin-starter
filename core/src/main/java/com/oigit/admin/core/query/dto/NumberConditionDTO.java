package com.oigit.admin.core.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "数值字段查询条件")
public class NumberConditionDTO<F extends Enum<F>> implements AbstractConditionNodeDTO {

    public enum NumberOperator {
        EQ,
        NE,
        GT,
        GTE,
        LT,
        LTE,
        BETWEEN,
        IS_NULL,
        IS_NOT_NULL
    }

    @NotNull
    @Schema(description = "数值字段")
    private F field;

    @NotNull
    @Schema(description = "数值操作符")
    private NumberOperator op;

    @Schema(description = "单值比较时使用")
    private BigDecimal value;

    @Schema(description = "BETWEEN 开始值")
    private BigDecimal start;

    @Schema(description = "BETWEEN 结束值")
    private BigDecimal end;

    @AssertTrue(message = "数值条件 value/start/end 不匹配操作符")
    public boolean isPayloadValid() {
        if (op == null || op == NumberOperator.IS_NULL || op == NumberOperator.IS_NOT_NULL) {
            return true;
        }
        if (op == NumberOperator.BETWEEN) {
            return start != null && end != null && start.compareTo(end) <= 0;
        }
        return value != null;
    }

    public F getField() { return field; }
    public void setField(F field) { this.field = field; }
    public NumberOperator getOp() { return op; }
    public void setOp(NumberOperator op) { this.op = op; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getStart() { return start; }
    public void setStart(BigDecimal start) { this.start = start; }
    public BigDecimal getEnd() { return end; }
    public void setEnd(BigDecimal end) { this.end = end; }
}
