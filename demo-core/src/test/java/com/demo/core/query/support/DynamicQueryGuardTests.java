package com.demo.core.query.support;

import com.demo.core.exception.BizException;
import com.demo.core.query.ast.ConditionGroupAst;
import com.demo.core.query.ast.ConditionLeafAst;
import com.demo.core.query.ast.QueryAst;
import com.demo.core.query.ast.QueryLogicOperator;
import com.demo.core.query.ast.QueryOperator;
import com.demo.core.query.ast.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicQueryGuardTests {

    private final DynamicQueryGuard guard = new DynamicQueryGuard(new QueryComplexityScorer());

    @Test
    void should_reject_depth_exceeded() {
        QueryAst queryAst = new QueryAst();
        queryAst.setRoot(group(group(group(group(group(leaf("NAME", QueryOperator.EQ, "status")))))));

        assertThatThrownBy(() -> guard.validate(queryAst, 20))
            .isInstanceOf(BizException.class);
    }

    @Test
    void should_reject_node_count_exceeded() {
        ConditionGroupAst group = new ConditionGroupAst();
        group.setLogic(QueryLogicOperator.AND);
        group.setChildren(IntStream.range(0, 21)
            .mapToObj(index -> leaf("NAME", QueryOperator.EQ, "v" + index))
            .toList());
        QueryAst queryAst = new QueryAst();
        queryAst.setRoot(group);

        assertThatThrownBy(() -> guard.validate(queryAst, 50))
            .isInstanceOf(BizException.class);
    }

    @Test
    void should_reject_in_size_exceeded() {
        QueryAst queryAst = new QueryAst();
        queryAst.setRoot(leaf(
            "STATUS",
            QueryOperator.IN,
            IntStream.range(0, 201).boxed().toList()
        ));

        assertThatThrownBy(() -> guard.validate(queryAst, 50))
            .isInstanceOf(BizException.class);
    }

    @Test
    void should_reject_page_size_exceeded() {
        QueryAst queryAst = new QueryAst();
        queryAst.setPageNo(1L);
        queryAst.setPageSize(2001L);

        assertThatThrownBy(() -> guard.validate(queryAst, 50))
            .isInstanceOf(BizException.class);
    }

    @Test
    void should_reject_complexity_exceeded() {
        ConditionGroupAst orGroup = new ConditionGroupAst();
        orGroup.setLogic(QueryLogicOperator.OR);
        orGroup.setChildren(List.of(
            leaf("NAME", QueryOperator.CONTAINS, "status"),
            leaf("NAME", QueryOperator.ENDS_WITH, "code")
        ));
        QueryAst queryAst = new QueryAst();
        queryAst.setRoot(orGroup);

        assertThatThrownBy(() -> guard.validate(queryAst, 3))
            .isInstanceOf(BizException.class);
    }

    private ConditionGroupAst group(com.demo.core.query.ast.ConditionAstNode child) {
        ConditionGroupAst group = new ConditionGroupAst();
        group.setLogic(QueryLogicOperator.AND);
        group.setChildren(List.of(child));
        return group;
    }

    private ConditionLeafAst leaf(String fieldKey, QueryOperator operator, Object typedValue) {
        ConditionLeafAst leaf = new ConditionLeafAst();
        leaf.setFieldKey(fieldKey);
        leaf.setOperator(operator);
        leaf.setTypedValue(typedValue);
        return leaf;
    }
}
