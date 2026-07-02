package com.demo.core.query.support;

import com.demo.core.exception.BizException;
import com.demo.core.query.ast.ConditionAstNode;
import com.demo.core.query.ast.ConditionGroupAst;
import com.demo.core.query.ast.ConditionLeafAst;
import com.demo.core.query.ast.QueryAst;
import com.demo.core.query.ast.QueryOperator;
import com.demo.core.query.exception.DynamicQueryErrorCode;
import com.demo.core.query.validation.DynamicQueryLimits;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class DynamicQueryGuard {

    private final QueryComplexityScorer scorer;

    public DynamicQueryGuard(QueryComplexityScorer scorer) {
        this.scorer = scorer;
    }

    public void validate(QueryAst queryAst, int maxComplexityScore) {
        Objects.requireNonNull(queryAst, "queryAst");
        if (queryAst.getPageSize() != null && queryAst.getPageSize() > DynamicQueryLimits.MAX_PAGE_SIZE) {
            throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_PAGE_SIZE_TOO_LARGE);
        }
        if (depth(queryAst.getRoot()) > DynamicQueryLimits.MAX_DEPTH) {
            throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_TREE_DEPTH_EXCEEDED);
        }
        if (countNodes(queryAst.getRoot()) > DynamicQueryLimits.MAX_NODE_COUNT) {
            throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_NODE_COUNT_EXCEEDED);
        }
        validateLeafPayload(queryAst.getRoot());
        if (scorer.score(queryAst.getRoot()) > maxComplexityScore) {
            throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_COMPLEXITY_EXCEEDED);
        }
    }

    private void validateLeafPayload(ConditionAstNode node) {
        if (node == null) {
            return;
        }
        if (node instanceof ConditionLeafAst leaf) {
            if (leaf.getOperator() == QueryOperator.IN && leaf.getTypedValue() instanceof List<?> values
                && values.size() > DynamicQueryLimits.MAX_IN_SIZE) {
                throw new BizException(DynamicQueryErrorCode.DYNAMIC_QUERY_IN_VALUE_TOO_LARGE);
            }
            return;
        }
        if (node instanceof ConditionGroupAst group && group.getChildren() != null) {
            for (ConditionAstNode child : group.getChildren()) {
                validateLeafPayload(child);
            }
        }
    }

    private int depth(ConditionAstNode node) {
        if (node == null) {
            return 0;
        }
        if (node instanceof ConditionLeafAst) {
            return 1;
        }
        ConditionGroupAst group = (ConditionGroupAst) node;
        if (group.getChildren() == null || group.getChildren().isEmpty()) {
            return 1;
        }
        return 1 + group.getChildren().stream().mapToInt(this::depth).max().orElse(0);
    }

    private int countNodes(ConditionAstNode node) {
        if (node == null) {
            return 0;
        }
        if (node instanceof ConditionLeafAst) {
            return 1;
        }
        ConditionGroupAst group = (ConditionGroupAst) node;
        if (group.getChildren() == null || group.getChildren().isEmpty()) {
            return 1;
        }
        return 1 + group.getChildren().stream().mapToInt(this::countNodes).sum();
    }
}
