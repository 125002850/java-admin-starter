package com.oigit.admin.operationaudit.infra.persistence.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.operationaudit.infra.persistence.entity.OperationAuditLogEntity;

public interface OperationAuditLogPersistenceService extends IService<OperationAuditLogEntity> {

    Page<OperationAuditLogEntity> pageBy(QueryAst queryAst);

    int maxQueryComplexityScore();
}
