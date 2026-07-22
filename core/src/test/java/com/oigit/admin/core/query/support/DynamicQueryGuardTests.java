package com.oigit.admin.core.query.support;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.ConditionGroupAst;
import com.oigit.admin.core.query.ast.ConditionLeafAst;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.ast.QueryLogicOperator;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicQueryGuardTests {

    private final DynamicQueryGuard guard = new DynamicQueryGuard(new QueryComplexityScorer());

    @Test
    void default_scene_complexity_score_should_be_100() {
        assertThat(new TestSceneQueryDefinition().maxComplexityScore()).isEqualTo(100);
    }

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
            IntStream.range(0, 1001).boxed().toList()
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

    private ConditionGroupAst group(com.oigit.admin.core.query.ast.ConditionAstNode child) {
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

    private static class TestSceneQueryDefinition implements SceneQueryDefinition<Object> {

        @Override
        public String sceneCode() {
            return "test.scene";
        }

        @Override
        public Map<String, SFunction<Object, String>> textFields() {
            return Map.of();
        }

        @Override
        public Map<String, SFunction<Object, LocalDateTime>> dateTimeFields() {
            return Map.of();
        }

        @Override
        public Map<String, SFunction<Object, ?>> enumFields() {
            return Map.of();
        }

        @Override
        public Map<String, SFunction<Object, ?>> sortFields() {
            return Map.of();
        }
    }
}
