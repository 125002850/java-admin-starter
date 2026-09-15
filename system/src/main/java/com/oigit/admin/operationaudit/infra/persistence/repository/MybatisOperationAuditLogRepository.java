package com.oigit.admin.operationaudit.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.audit.OperationAuditAction;
import com.oigit.admin.core.audit.OperationAuditResultStatus;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.operationaudit.domain.model.OperationAuditLog;
import com.oigit.admin.operationaudit.domain.model.OperationAuditLogPage;
import com.oigit.admin.operationaudit.domain.repository.OperationAuditLogRepository;
import com.oigit.admin.operationaudit.infra.persistence.entity.OperationAuditLogEntity;
import com.oigit.admin.operationaudit.infra.persistence.service.OperationAuditLogPersistenceService;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisOperationAuditLogRepository implements OperationAuditLogRepository {

    private final OperationAuditLogPersistenceService persistenceService;

    public MybatisOperationAuditLogRepository(OperationAuditLogPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public void create(OperationAuditLog log) {
        persistenceService.save(toEntity(log));
    }

    @Override
    public OperationAuditLogPage page(QueryAst queryAst) {
        Page<OperationAuditLogEntity> page = persistenceService.pageBy(queryAst);
        return new OperationAuditLogPage(page.getRecords().stream().map(this::toDomain).toList(), page.getTotal());
    }

    @Override
    public Optional<OperationAuditLog> findActiveById(Long logId) {
        return Optional.ofNullable(persistenceService.getOne(Wrappers.<OperationAuditLogEntity>lambdaQuery()
                        .eq(OperationAuditLogEntity::getId, logId)
                        .eq(OperationAuditLogEntity::getDeleted, 0L)))
                .map(this::toDomain);
    }

    @Override
    public int maxQueryComplexityScore() {
        return persistenceService.maxQueryComplexityScore();
    }

    private OperationAuditLogEntity toEntity(OperationAuditLog log) {
        OperationAuditLogEntity entity = new OperationAuditLogEntity();
        entity.setModuleCode(log.moduleCode());
        entity.setModuleName(log.moduleName());
        entity.setAction(log.action().getCode());
        entity.setDescription(log.description());
        entity.setOperatorId(log.operatorId());
        entity.setOperatorName(log.operatorName());
        entity.setOperatorUsername(log.operatorUsername());
        entity.setOperatorRealName(log.operatorRealName());
        entity.setRequestMethod(log.requestMethod());
        entity.setRequestPath(log.requestPath());
        entity.setClientIp(log.clientIp());
        entity.setTraceId(log.traceId());
        entity.setRequestParams(log.requestParams());
        entity.setResultStatus(log.resultStatus().getCode());
        entity.setHttpStatus(log.httpStatus());
        entity.setResultCode(log.resultCode());
        entity.setErrorMessage(log.errorMessage());
        entity.setDurationMs(log.durationMs());
        entity.setOperationTime(log.operationTime());
        return entity;
    }

    private OperationAuditLog toDomain(OperationAuditLogEntity entity) {
        return new OperationAuditLog(
                entity.getId(),
                entity.getModuleCode(),
                entity.getModuleName(),
                OperationAuditAction.fromCode(entity.getAction()),
                entity.getDescription(),
                entity.getOperatorId(),
                entity.getOperatorName(),
                entity.getOperatorUsername(),
                entity.getOperatorRealName(),
                entity.getRequestMethod(),
                entity.getRequestPath(),
                entity.getClientIp(),
                entity.getTraceId(),
                entity.getRequestParams(),
                OperationAuditResultStatus.fromCode(entity.getResultStatus()),
                entity.getHttpStatus(),
                entity.getResultCode(),
                entity.getErrorMessage(),
                entity.getDurationMs(),
                entity.getOperationTime()
        );
    }
}
