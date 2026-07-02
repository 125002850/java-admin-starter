package com.demo.core.query.dto;

import com.demo.core.query.validation.DynamicQueryLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "动态查询条件请求基类")
public class BaseDynamicCriteriaReqDTO<N extends AbstractConditionNodeDTO, S extends SortItemDTO<?>> {

    @Valid
    @Schema(description = "查询条件树")
    private N condition;

    @Valid
    @Size(max = DynamicQueryLimits.MAX_SORT_SIZE)
    @Schema(description = "排序项")
    private List<S> sort;

    public N getCondition() {
        return condition;
    }

    public void setCondition(N condition) {
        this.condition = condition;
    }

    public List<S> getSort() {
        return sort;
    }

    public void setSort(List<S> sort) {
        this.sort = sort;
    }
}
