package com.oigit.admin.mdm.export.query;

import com.oigit.admin.core.query.ast.ConditionAstNode;
import com.oigit.admin.core.query.ast.ConditionGroupAst;
import com.oigit.admin.core.query.ast.ConditionLeafAst;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.ast.QueryLogicOperator;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.scene.DynamicQueryAstMapper;
import com.oigit.admin.core.query.scene.SceneQueryMapper;
import com.oigit.admin.mdm.export.controller.dto.query.ExportRecordDynamicCriteriaReqDTO;
import com.oigit.admin.mdm.export.controller.dto.query.ExportRecordDynamicPageReqDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExportRecordSceneQueryMapper implements SceneQueryMapper<ExportRecordDynamicCriteriaReqDTO> {

    @Override
    public QueryAst toQueryAst(ExportRecordDynamicCriteriaReqDTO reqDTO) {
        return DynamicQueryAstMapper.toQueryAst(reqDTO.getCondition(), reqDTO.getSort());
    }

    public QueryAst map(ExportRecordDynamicPageReqDTO reqDTO, Long ownerId) {
        QueryAst queryAst = DynamicQueryAstMapper.toPageQueryAst(reqDTO);
        queryAst.setRoot(mergeOwnerConstraint(queryAst.getRoot(), ownerId));
        return queryAst;
    }

    private ConditionAstNode mergeOwnerConstraint(ConditionAstNode currentRoot, Long ownerId) {
        ConditionLeafAst ownerConstraint = new ConditionLeafAst();
        ownerConstraint.setFieldKey(ExportRecordSceneQueryDefinition.OWNER_ID_FIELD);
        ownerConstraint.setOperator(QueryOperator.EQ);
        ownerConstraint.setTypedValue(ownerId);
        if (currentRoot == null) {
            return ownerConstraint;
        }
        ConditionGroupAst groupAst = new ConditionGroupAst();
        groupAst.setLogic(QueryLogicOperator.AND);
        groupAst.setChildren(List.of(ownerConstraint, currentRoot));
        return groupAst;
    }
}
