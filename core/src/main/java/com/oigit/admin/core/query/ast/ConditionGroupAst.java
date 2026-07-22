package com.oigit.admin.core.query.ast;

import java.util.List;

public class ConditionGroupAst extends ConditionAstNode {

    private QueryLogicOperator logic;
    private List<ConditionAstNode> children;

    public QueryLogicOperator getLogic() {
        return logic;
    }

    public void setLogic(QueryLogicOperator logic) {
        this.logic = logic;
    }

    public List<ConditionAstNode> getChildren() {
        return children;
    }

    public void setChildren(List<? extends ConditionAstNode> children) {
        this.children = children == null ? null : List.copyOf(children);
    }
}
