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
    public Map<String, String> fieldLabels() {
        return Map.ofEntries(
                Map.entry("moduleCode", "模块编码"), Map.entry("moduleName", "模块名称"),
                Map.entry("description", "操作描述"), Map.entry("operatorName", "操作人"),
                Map.entry("clientIp", "客户端 IP"), Map.entry("traceId", "追踪 ID"),
                Map.entry("requestPath", "请求路径"), Map.entry("operationTime", "操作时间"),
                Map.entry("action", "操作类型"), Map.entry("resultStatus", "操作结果"),
                Map.entry("logId", "日志 ID"), Map.entry("operatorId", "操作人 ID"),
                Map.entry("durationMs", "耗时（毫秒）"), Map.entry("httpStatus", "HTTP 状态码"),
                Map.entry("resultCode", "业务状态码"));
    }

    @Override
    public Map<String, Map<String, String>> valueLabels() {
        return Map.of(
                "action", java.util.Arrays.stream(com.oigit.admin.core.audit.OperationAuditAction.values())
                        .collect(java.util.stream.Collectors.toMap(com.oigit.admin.core.audit.OperationAuditAction::getCode,
                                com.oigit.admin.core.audit.OperationAuditAction::getDesc)),
                "resultStatus", java.util.Arrays.stream(com.oigit.admin.core.audit.OperationAuditResultStatus.values())
                        .collect(java.util.stream.Collectors.toMap(com.oigit.admin.core.audit.OperationAuditResultStatus::getCode,
                                com.oigit.admin.core.audit.OperationAuditResultStatus::getDesc)));
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
