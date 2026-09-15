package com.oigit.admin.operationaudit.app.query;

import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.scene.DynamicQueryAstMapper;
import com.oigit.admin.core.query.scene.SceneQueryMapper;
import com.oigit.admin.operationaudit.dto.req.query.OperationAuditLogDynamicCriteriaReqDTO;
import com.oigit.admin.operationaudit.dto.req.query.OperationAuditLogDynamicPageReqDTO;
import org.springframework.stereotype.Component;

@Component
public class OperationAuditLogSceneQueryMapper
        implements SceneQueryMapper<OperationAuditLogDynamicCriteriaReqDTO> {

    @Override
    public QueryAst toQueryAst(OperationAuditLogDynamicCriteriaReqDTO reqDTO) {
        return DynamicQueryAstMapper.toQueryAst(reqDTO.getCondition(), reqDTO.getSort());
    }

    public QueryAst map(OperationAuditLogDynamicPageReqDTO reqDTO) {
        return DynamicQueryAstMapper.toPageQueryAst(reqDTO);
    }
}
