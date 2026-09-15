package com.oigit.admin.operationaudit.domain.repository;

import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.operationaudit.domain.model.OperationAuditLog;
import com.oigit.admin.operationaudit.domain.model.OperationAuditLogPage;

import java.util.Optional;

public interface OperationAuditLogRepository {

    void create(OperationAuditLog log);

    OperationAuditLogPage page(QueryAst queryAst);

    Optional<OperationAuditLog> findActiveById(Long logId);

    int maxQueryComplexityScore();
}
