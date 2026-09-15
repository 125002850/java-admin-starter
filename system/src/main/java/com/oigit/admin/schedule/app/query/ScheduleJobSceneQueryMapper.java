package com.oigit.admin.schedule.app.query;

import com.oigit.admin.core.query.ast.ConditionAstNode;
import com.oigit.admin.core.query.ast.ConditionGroupAst;
import com.oigit.admin.core.query.ast.ConditionLeafAst;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.ast.QueryLogicOperator;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.scene.DynamicQueryAstMapper;
import com.oigit.admin.core.query.scene.SceneQueryMapper;
import com.oigit.admin.schedule.dto.req.query.ScheduleJobDynamicCriteriaReqDTO;
import com.oigit.admin.schedule.dto.req.query.ScheduleJobPageQueryReqDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduleJobSceneQueryMapper implements SceneQueryMapper<ScheduleJobDynamicCriteriaReqDTO> {

    @Override
    public QueryAst toQueryAst(ScheduleJobDynamicCriteriaReqDTO reqDTO) {
        return DynamicQueryAstMapper.toQueryAst(reqDTO.getCondition(), reqDTO.getSort());
    }

    public QueryAst map(ScheduleJobPageQueryReqDTO reqDTO) {
        QueryAst queryAst = DynamicQueryAstMapper.toPageQueryAst(reqDTO);
        queryAst.setRoot(mergeKeywordConstraint(queryAst.getRoot(), reqDTO.getKeyword()));
        return queryAst;
    }

    private ConditionAstNode mergeKeywordConstraint(ConditionAstNode currentRoot, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return currentRoot;
        }
        ConditionLeafAst nameLeaf = new ConditionLeafAst();
        nameLeaf.setFieldKey("jobName");
        nameLeaf.setOperator(QueryOperator.CONTAINS);
        nameLeaf.setTypedValue(keyword);

        ConditionLeafAst codeLeaf = new ConditionLeafAst();
        codeLeaf.setFieldKey("jobCode");
        codeLeaf.setOperator(QueryOperator.CONTAINS);
        codeLeaf.setTypedValue(keyword);

        ConditionLeafAst groupLeaf = new ConditionLeafAst();
        groupLeaf.setFieldKey("groupName");
        groupLeaf.setOperator(QueryOperator.CONTAINS);
        groupLeaf.setTypedValue(keyword);

        ConditionGroupAst keywordGroup = new ConditionGroupAst();
        keywordGroup.setLogic(QueryLogicOperator.OR);
        keywordGroup.setChildren(List.of(nameLeaf, codeLeaf, groupLeaf));

        if (currentRoot == null) {
            return keywordGroup;
        }
        ConditionGroupAst andGroup = new ConditionGroupAst();
        andGroup.setLogic(QueryLogicOperator.AND);
        andGroup.setChildren(List.of(keywordGroup, currentRoot));
        return andGroup;
    }
}
