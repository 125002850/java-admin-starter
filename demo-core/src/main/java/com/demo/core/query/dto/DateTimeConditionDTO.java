package com.demo.core.query.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.util.StringUtils;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "时间字段查询条件")
public class DateTimeConditionDTO<F extends Enum<F>> implements AbstractConditionNodeDTO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public enum DateTimeOperator {
        GT,
        GTE,
        LT,
        LTE,
        BETWEEN,
        IS_NULL,
        IS_NOT_NULL
    }

    @NotNull
    @Schema(description = "时间字段")
    private F field;

    @NotNull
    @Schema(description = "时间操作符")
    private DateTimeOperator op;

    @Schema(description = "单值比较时使用，格式 yyyy-MM-dd HH:mm:ss")
    private String value;

    @Schema(description = "BETWEEN 开始值，格式 yyyy-MM-dd HH:mm:ss")
    private String start;

    @Schema(description = "BETWEEN 结束值，格式 yyyy-MM-dd HH:mm:ss")
    private String end;

    @AssertTrue(message = "时间条件 value/start/end 不匹配操作符")
    public boolean isPayloadValid() {
        if (op == null) {
            return true;
        }
        if (op == DateTimeOperator.BETWEEN) {
            return isParsable(start)
                    && isParsable(end)
                    && parse(start).compareTo(parse(end)) <= 0;
        }
        if (op == DateTimeOperator.IS_NULL || op == DateTimeOperator.IS_NOT_NULL) {
            return true;
        }
        return isParsable(value);
    }

    private boolean isParsable(String source) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        try {
            parse(source);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private LocalDateTime parse(String source) {
        return LocalDateTime.parse(source, DATE_TIME_FORMATTER);
    }

    public F getField() {
        return field;
    }

    public void setField(F field) {
        this.field = field;
    }

    public DateTimeOperator getOp() {
        return op;
    }

    public void setOp(DateTimeOperator op) {
        this.op = op;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
