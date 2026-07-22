package com.oigit.admin.core.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.StringUtils;

import java.util.List;

@Schema(description = "文本字段查询条件")
public class TextConditionDTO<F extends Enum<F>> implements AbstractConditionNodeDTO {

    public enum TextOperator {
        EQ,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        IN,
        IS_NULL,
        IS_NOT_NULL
    }

    @NotNull
    @Schema(description = "文本字段")
    private F field;

    @NotNull
    @Schema(description = "文本操作符")
    private TextOperator op;

    @Schema(description = "单值操作符使用")
    private String value;

    @Schema(description = "IN 操作符使用")
    private List<String> values;

    @AssertTrue(message = "文本条件 value/values 不匹配操作符")
    public boolean isPayloadValid() {
        if (op == null) {
            return true;
        }
        if (op == TextOperator.IN) {
            return values != null
                && !values.isEmpty()
                && values.stream().allMatch(StringUtils::hasText);
        }
        if (op == TextOperator.IS_NULL || op == TextOperator.IS_NOT_NULL) {
            return true;
        }
        return StringUtils.hasText(value);
    }

    public F getField() {
        return field;
    }

    public void setField(F field) {
        this.field = field;
    }

    public TextOperator getOp() {
        return op;
    }

    public void setOp(TextOperator op) {
        this.op = op;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
