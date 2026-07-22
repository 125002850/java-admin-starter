package com.oigit.admin.core.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "动态查询排序项")
public class SortItemDTO<F extends Enum<F>> {

    public enum SortDirection {
        ASC,
        DESC
    }

    @NotNull
    @Schema(description = "排序字段")
    private F field;

    @NotNull
    @Schema(description = "排序方向")
    private SortDirection direction;

    public F getField() {
        return field;
    }

    public void setField(F field) {
        this.field = field;
    }

    public SortDirection getDirection() {
        return direction;
    }

    public void setDirection(SortDirection direction) {
        this.direction = direction;
    }
}
