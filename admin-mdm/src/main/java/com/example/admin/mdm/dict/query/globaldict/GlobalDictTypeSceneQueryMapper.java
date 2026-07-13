package com.example.admin.mdm.dict.query.globaldict;

import com.example.admin.core.query.ast.ConditionAstNode;
import com.example.admin.core.query.ast.ConditionGroupAst;
import com.example.admin.core.query.ast.ConditionLeafAst;
import com.example.admin.core.query.ast.QueryAst;
import com.example.admin.core.query.ast.QueryLogicOperator;
import com.example.admin.core.query.ast.QueryOperator;
import com.example.admin.core.query.scene.DynamicQueryAstMapper;
import com.example.admin.core.query.scene.SceneQueryMapper;
import com.example.admin.mdm.dict.controller.dto.query.GlobalDictTypeDynamicCriteriaReqDTO;
import com.example.admin.mdm.dict.controller.dto.query.GlobalDictTypeDynamicListReqDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GlobalDictTypeSceneQueryMapper implements SceneQueryMapper<GlobalDictTypeDynamicCriteriaReqDTO> {

    @Override
    public QueryAst toQueryAst(GlobalDictTypeDynamicCriteriaReqDTO reqDTO) {
        return DynamicQueryAstMapper.toQueryAst(reqDTO.getCondition(), reqDTO.getSort());
    }

    public QueryAst map(GlobalDictTypeDynamicListReqDTO reqDTO) {
        QueryAst queryAst = DynamicQueryAstMapper.toPageQueryAst(reqDTO);
        queryAst.setRoot(mergeKeywordConstraint(queryAst.getRoot(), reqDTO.getKeyword()));
        return queryAst;
    }

    private ConditionAstNode mergeKeywordConstraint(ConditionAstNode currentRoot, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return currentRoot;
        }
        ConditionLeafAst codeLeaf = new ConditionLeafAst();
        codeLeaf.setFieldKey("dictTypeCode");
        codeLeaf.setOperator(QueryOperator.CONTAINS);
        codeLeaf.setTypedValue(keyword);

        ConditionLeafAst nameLeaf = new ConditionLeafAst();
        nameLeaf.setFieldKey("dictTypeName");
        nameLeaf.setOperator(QueryOperator.CONTAINS);
        nameLeaf.setTypedValue(keyword);

        ConditionGroupAst keywordGroup = new ConditionGroupAst();
        keywordGroup.setLogic(QueryLogicOperator.OR);
        keywordGroup.setChildren(List.of(codeLeaf, nameLeaf));

        if (currentRoot == null) {
            return keywordGroup;
        }
        ConditionGroupAst andGroup = new ConditionGroupAst();
        andGroup.setLogic(QueryLogicOperator.AND);
        andGroup.setChildren(List.of(keywordGroup, currentRoot));
        return andGroup;
    }
}
