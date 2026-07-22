package com.oigit.admin.core.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "动态查询逻辑分组节点")
public class ConditionGroupDTO<N extends AbstractConditionNodeDTO> implements AbstractConditionNodeDTO {

    public enum LogicOperator {
        AND,
        OR
    }

    @NotNull
    @Schema(description = "分组逻辑")
    private LogicOperator logic;

    @Valid
    @NotEmpty
    @Schema(description = "子节点列表")
    private List<N> children;

    public LogicOperator getLogic() {
        return logic;
    }

    public void setLogic(LogicOperator logic) {
        this.logic = logic;
    }

    public List<N> getChildren() {
        return children;
    }

    public void setChildren(List<N> children) {
        this.children = children;
    }
}
