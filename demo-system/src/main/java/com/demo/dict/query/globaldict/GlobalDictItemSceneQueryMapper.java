package com.demo.dict.query.globaldict;

import com.demo.core.query.ast.QueryAst;
import com.demo.core.query.scene.DynamicQueryAstMapper;
import com.demo.core.query.scene.SceneQueryMapper;
import com.demo.dict.controller.dto.query.GlobalDictItemDynamicCriteriaReqDTO;
import com.demo.dict.controller.dto.query.GlobalDictItemDynamicPageReqDTO;
import org.springframework.stereotype.Component;

@Component
public class GlobalDictItemSceneQueryMapper implements SceneQueryMapper<GlobalDictItemDynamicCriteriaReqDTO> {

    @Override
    public QueryAst toQueryAst(GlobalDictItemDynamicCriteriaReqDTO reqDTO) {
        return DynamicQueryAstMapper.toQueryAst(reqDTO.getCondition(), reqDTO.getSort());
    }

    public QueryAst map(GlobalDictItemDynamicPageReqDTO reqDTO) {
        QueryAst queryAst = DynamicQueryAstMapper.toQueryAst(reqDTO.getCondition(), reqDTO.getSort());
        queryAst.setPageNo(reqDTO.getPageNo());
        queryAst.setPageSize(reqDTO.getPageSize());
        return queryAst;
    }
}
