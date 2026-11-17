package com.oigit.admin.core.query.support;

import com.oigit.admin.core.query.ast.ConditionGroupAst;
import com.oigit.admin.core.query.ast.ConditionLeafAst;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.ast.QueryLogicOperator;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.core.query.scene.QueryFieldSpec;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicQuerySummaryRendererTests {

    private final DynamicQuerySummaryRenderer renderer = new DynamicQuerySummaryRenderer();

    @Test
    void render_should_translate_fields_operators_values_and_groups() {
        QueryAst queryAst = new QueryAst();
        ConditionGroupAst group = new ConditionGroupAst();
        group.setLogic(QueryLogicOperator.AND);
        group.setChildren(List.of(
                leaf("riskLevel", QueryOperator.IN, List.of("HIGH", "MEDIUM")),
                leaf("isSafe", QueryOperator.EQ, "1"),
                leaf(
                        "createTime",
                        QueryOperator.BETWEEN,
                        List.of(
                                LocalDateTime.of(2026, 6, 1, 0, 0, 0),
                                LocalDateTime.of(2026, 6, 30, 23, 59, 59)
                        )
                )
        ));
        queryAst.setRoot(group);

        String summary = renderer.render(queryAst, new TestSceneQueryDefinition());

        assertThat(summary).isEqualTo(
                "风险等级 属于 高风险、中风险 且 是否安全 等于 是 且 创建时间 介于 2026-06-01 00:00:00 至 2026-06-30 23:59:59"
        );
    }

    private ConditionLeafAst leaf(String fieldKey, QueryOperator operator, Object typedValue) {
        ConditionLeafAst leaf = new ConditionLeafAst();
        leaf.setFieldKey(fieldKey);
        leaf.setOperator(operator);
        leaf.setTypedValue(typedValue);
        return leaf;
    }

    private static class TestSceneQueryDefinition implements SceneQueryDefinition<Object> {
        public String sceneCode() { return "test.scene"; }
        public Map<String, QueryFieldSpec<Object>> fields() {
            return Map.of(
                    "riskLevel", QueryFieldSpec.<Object>text(row -> "", "风险等级")
                            .withValueLabels(Map.of("HIGH", "高风险", "MEDIUM", "中风险")),
                    "isSafe", QueryFieldSpec.<Object>text(row -> "", "是否安全")
                            .withValueLabels(Map.of("1", "是", "0", "否")),
                    "createTime", QueryFieldSpec.dateTime(row -> null, "创建时间"));
        }
    }
}
