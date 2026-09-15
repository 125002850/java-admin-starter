package com.oigit.admin.operationaudit.infra.query;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.ast.SortSpec;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.operationaudit.infra.persistence.entity.OperationAuditLogEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class OperationAuditLogSceneQueryDefinition implements SceneQueryDefinition<OperationAuditLogEntity> {

    @Override
    public String sceneCode() {
        return "system.operation-audit.log.page";
    }

    @Override
    public Map<String, SFunction<OperationAuditLogEntity, String>> textFields() {
        return Map.of(
                "moduleCode", OperationAuditLogEntity::getModuleCode,
                "moduleName", OperationAuditLogEntity::getModuleName,
                "description", OperationAuditLogEntity::getDescription,
                "operatorName", OperationAuditLogEntity::getOperatorName,
                "clientIp", OperationAuditLogEntity::getClientIp,
                "traceId", OperationAuditLogEntity::getTraceId,
                "requestPath", OperationAuditLogEntity::getRequestPath
        );
    }

    @Override
    public Map<String, SFunction<OperationAuditLogEntity, LocalDateTime>> dateTimeFields() {
        return Map.of("operationTime", OperationAuditLogEntity::getOperationTime);
    }

    @Override
    public Map<String, SFunction<OperationAuditLogEntity, ?>> enumFields() {
        return Map.of(
                "action", OperationAuditLogEntity::getAction,
                "resultStatus", OperationAuditLogEntity::getResultStatus
        );
    }

    @Override
    public Map<String, SFunction<OperationAuditLogEntity, ? extends Number>> numberFields() {
        return Map.of(
                "logId", OperationAuditLogEntity::getId,
                "operatorId", OperationAuditLogEntity::getOperatorId,
                "durationMs", OperationAuditLogEntity::getDurationMs,
                "httpStatus", OperationAuditLogEntity::getHttpStatus,
                "resultCode", OperationAuditLogEntity::getResultCode
        );
    }

    @Override
    public Map<String, SFunction<OperationAuditLogEntity, ?>> sortFields() {
        return Map.of(
                "logId", OperationAuditLogEntity::getId,
                "operationTime", OperationAuditLogEntity::getOperationTime,
                "moduleName", OperationAuditLogEntity::getModuleName,
                "action", OperationAuditLogEntity::getAction,
                "operatorName", OperationAuditLogEntity::getOperatorName,
                "resultStatus", OperationAuditLogEntity::getResultStatus,
                "durationMs", OperationAuditLogEntity::getDurationMs
        );
    }

    @Override
    public Set<QueryOperator> allowedOperators(String fieldKey) {
        return SceneQueryDefinition.super.allowedOperators(fieldKey);
    }

    @Override
    public List<SortSpec> defaultSorts() {
        return List.of(
                new SortSpec("operationTime", SortItemDTO.SortDirection.DESC),
                new SortSpec("logId", SortItemDTO.SortDirection.DESC)
        );
    }
}
