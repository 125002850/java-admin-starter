package com.oigit.admin.core.query.executor;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.ConditionGroupAst;
import com.oigit.admin.core.query.ast.ConditionLeafAst;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.ast.QueryLogicOperator;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.ast.SortSpec;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MybatisPlusQueryExecutorTests {

    private final MybatisPlusQueryExecutor executor = new MybatisPlusQueryExecutor();

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MybatisMapperBuilderAssistant assistant = new MybatisMapperBuilderAssistant(configuration, "admin-scene");
        assistant.setCurrentNamespace(SampleEntity.class.getName());
        TableInfoHelper.initTableInfo(assistant, SampleEntity.class);
    }

    @Test
    void select_page_should_translate_filters_page_and_default_sort() {
        @SuppressWarnings("unchecked")
        BaseMapper<SampleEntity> mapper = mock(BaseMapper.class);
        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        QueryAst queryAst = new QueryAst();
        queryAst.setPageNo(2L);
        queryAst.setPageSize(10L);
        queryAst.setRoot(and(
            leaf("NAME", QueryOperator.CONTAINS, "status"),
            leaf("STATUS", QueryOperator.EQ, 2),
            leaf("CREATE_TIME", QueryOperator.BETWEEN, List.of(
                LocalDateTime.of(2026, 6, 1, 0, 0, 0),
                LocalDateTime.of(2026, 6, 30, 23, 59, 59)
            ))
        ));

        Page<SampleEntity> page = executor.selectPage(mapper, queryAst, sceneDefinition());

        ArgumentCaptor<Page<SampleEntity>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<SampleEntity>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());

        assertThat(page.getCurrent()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(wrapperCaptor.getValue().getSqlSegment())
            .contains("name")
            .contains("status")
            .contains("create_time")
            .contains("ORDER BY id ASC");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
            .contains("%status%", 2);
    }

    @Test
    void select_list_should_apply_explicit_sort() {
        @SuppressWarnings("unchecked")
        BaseMapper<SampleEntity> mapper = mock(BaseMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        QueryAst queryAst = new QueryAst();
        queryAst.setSorts(List.of(new SortSpec("CREATE_TIME", SortItemDTO.SortDirection.DESC)));
        queryAst.setRoot(leaf("NAME", QueryOperator.STARTS_WITH, "sys"));

        executor.selectList(mapper, queryAst, sceneDefinition());

        ArgumentCaptor<LambdaQueryWrapper<SampleEntity>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("ORDER BY create_time DESC");
    }

    @Test
    void select_list_should_apply_null_operators() {
        @SuppressWarnings("unchecked")
        BaseMapper<SampleEntity> mapper = mock(BaseMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        QueryAst queryAst = new QueryAst();
        queryAst.setRoot(and(
            leaf("NAME", QueryOperator.IS_NULL, null),
            leaf("STATUS", QueryOperator.IS_NOT_NULL, null)
        ));

        executor.selectList(mapper, queryAst, sceneDefinition());

        ArgumentCaptor<LambdaQueryWrapper<SampleEntity>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
            .contains("name IS NULL")
            .contains("status IS NOT NULL");
    }

    @Test
    void should_reject_operator_outside_scene_allowlist() {
        @SuppressWarnings("unchecked")
        BaseMapper<SampleEntity> mapper = mock(BaseMapper.class);
        QueryAst queryAst = new QueryAst();
        queryAst.setRoot(leaf("STATUS", QueryOperator.CONTAINS, "bad"));

        assertThatThrownBy(() -> executor.selectList(mapper, queryAst, sceneDefinition()))
            .isInstanceOf(BizException.class);
    }

    @Test
    void nested_group_should_keep_parentheses_for_first_child() {
        @SuppressWarnings("unchecked")
        BaseMapper<SampleEntity> mapper = mock(BaseMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        ConditionGroupAst orGroup = new ConditionGroupAst();
        orGroup.setLogic(QueryLogicOperator.OR);
        orGroup.setChildren(List.of(
            leaf("NAME", QueryOperator.EQ, "sys"),
            leaf("NAME", QueryOperator.EQ, "biz")
        ));

        QueryAst queryAst = new QueryAst();
        queryAst.setRoot(group(QueryLogicOperator.AND, orGroup, leaf("STATUS", QueryOperator.EQ, 1)));

        executor.selectList(mapper, queryAst, sceneDefinition());

        ArgumentCaptor<LambdaQueryWrapper<SampleEntity>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).startsWith("(");
    }

    private ConditionGroupAst and(ConditionLeafAst... children) {
        return group(QueryLogicOperator.AND, children);
    }

    private ConditionGroupAst group(QueryLogicOperator logic, com.oigit.admin.core.query.ast.ConditionAstNode... children) {
        ConditionGroupAst group = new ConditionGroupAst();
        group.setLogic(logic);
        group.setChildren(List.of(children));
        return group;
    }

    private ConditionLeafAst leaf(String fieldKey, QueryOperator operator, Object typedValue) {
        ConditionLeafAst leaf = new ConditionLeafAst();
        leaf.setFieldKey(fieldKey);
        leaf.setOperator(operator);
        leaf.setTypedValue(typedValue);
        return leaf;
    }

    private SceneQueryDefinition<SampleEntity> sceneDefinition() {
        return new SceneQueryDefinition<>() {
            @Override
            public String sceneCode() {
                return "sample.scene";
            }

            @Override
            public Map<String, SFunction<SampleEntity, String>> textFields() {
                return Map.of("NAME", SampleEntity::getName);
            }

            @Override
            public Map<String, SFunction<SampleEntity, LocalDateTime>> dateTimeFields() {
                return Map.of("CREATE_TIME", SampleEntity::getCreateTime);
            }

            @Override
            public Map<String, SFunction<SampleEntity, ?>> enumFields() {
                return Map.of("STATUS", SampleEntity::getStatus);
            }

            @Override
            public Map<String, SFunction<SampleEntity, ?>> sortFields() {
                return Map.of(
                    "ID", SampleEntity::getId,
                    "CREATE_TIME", SampleEntity::getCreateTime
                );
            }

            @Override
            public Set<QueryOperator> allowedOperators(String fieldKey) {
                return switch (fieldKey) {
                    case "NAME" -> Set.of(
                        QueryOperator.EQ,
                        QueryOperator.CONTAINS,
                        QueryOperator.STARTS_WITH,
                        QueryOperator.ENDS_WITH,
                        QueryOperator.IN,
                        QueryOperator.IS_NULL,
                        QueryOperator.IS_NOT_NULL
                    );
                    case "STATUS" -> Set.of(
                        QueryOperator.EQ,
                        QueryOperator.IN,
                        QueryOperator.IS_NULL,
                        QueryOperator.IS_NOT_NULL
                    );
                    case "CREATE_TIME" -> Set.of(
                        QueryOperator.GT,
                        QueryOperator.GTE,
                        QueryOperator.LT,
                        QueryOperator.LTE,
                        QueryOperator.BETWEEN,
                        QueryOperator.IS_NULL,
                        QueryOperator.IS_NOT_NULL
                    );
                    default -> Set.of();
                };
            }

            @Override
            public List<SortSpec> defaultSorts() {
                return List.of(new SortSpec("ID", SortItemDTO.SortDirection.ASC));
            }
        };
    }

    static class SampleEntity {
        private Long id;
        private String name;
        private Integer status;
        private LocalDateTime createTime;

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getStatus() {
            return status;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }
    }
}
