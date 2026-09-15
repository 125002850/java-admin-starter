package com.oigit.admin.operationaudit.infra.persistence.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.operationaudit.infra.persistence.entity.OperationAuditLogEntity;
import com.oigit.admin.operationaudit.infra.persistence.mapper.OperationAuditLogMapper;
import com.oigit.admin.operationaudit.infra.persistence.service.OperationAuditLogPersistenceService;
import com.oigit.admin.operationaudit.infra.query.OperationAuditLogSceneQueryDefinition;
import org.springframework.stereotype.Service;

@Service
public class OperationAuditLogPersistenceServiceImpl
        extends ServiceImpl<OperationAuditLogMapper, OperationAuditLogEntity>
        implements OperationAuditLogPersistenceService {

    private final MybatisPlusQueryExecutor queryExecutor;
    private final OperationAuditLogSceneQueryDefinition queryDefinition;

    public OperationAuditLogPersistenceServiceImpl(
            MybatisPlusQueryExecutor queryExecutor,
            OperationAuditLogSceneQueryDefinition queryDefinition
    ) {
        this.queryExecutor = queryExecutor;
        this.queryDefinition = queryDefinition;
    }

    @Override
    public Page<OperationAuditLogEntity> pageBy(QueryAst queryAst) {
        return queryExecutor.selectPage(getBaseMapper(), queryAst, queryDefinition);
    }

    @Override
    public int maxQueryComplexityScore() {
        return queryDefinition.maxComplexityScore();
    }
}
