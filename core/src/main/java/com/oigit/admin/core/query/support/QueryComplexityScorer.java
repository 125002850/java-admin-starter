package com.oigit.admin.core.query.support;

import com.oigit.admin.core.query.ast.ConditionAstNode;
import com.oigit.admin.core.query.ast.ConditionGroupAst;
import com.oigit.admin.core.query.ast.ConditionLeafAst;
import com.oigit.admin.core.query.ast.QueryLogicOperator;
import com.oigit.admin.core.query.ast.QueryOperator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryComplexityScorer {

    public int score(ConditionAstNode node) {
        if (node == null) {
            return 0;
        }
        if (node instanceof ConditionLeafAst leaf) {
            return scoreLeaf(leaf);
        }
        if (node instanceof ConditionGroupAst group) {
            int groupWeight = group.getLogic() == QueryLogicOperator.OR ? 2 : 0;
            List<ConditionAstNode> children = group.getChildren();
            if (children == null || children.isEmpty()) {
                return groupWeight;
            }
            return groupWeight + children.stream().mapToInt(this::score).sum();
        }
        return 0;
    }

    private int scoreLeaf(ConditionLeafAst leaf) {
        int score = 1;
        if (leaf.getOperator() == QueryOperator.CONTAINS || leaf.getOperator() == QueryOperator.ENDS_WITH) {
            score += 2;
        }
        if (leaf.getOperator() == QueryOperator.BETWEEN) {
            score += 1;
        }
        if (leaf.getOperator() == QueryOperator.IN && leaf.getTypedValue() instanceof List<?> values) {
            score += 1 + (values.size() / 20);
        }
        return score;
    }
}
