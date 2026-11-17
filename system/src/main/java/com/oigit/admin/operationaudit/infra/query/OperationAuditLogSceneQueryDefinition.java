package com.oigit.admin.operationaudit.infra.query;

import com.oigit.admin.core.query.ast.SortSpec;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.core.query.scene.QueryFieldSpec;
import com.oigit.admin.operationaudit.infra.persistence.entity.OperationAuditLogEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OperationAuditLogSceneQueryDefinition implements SceneQueryDefinition<OperationAuditLogEntity> {

    @Override
    public String sceneCode() {
        return "system.operation-audit.log.page";
    }

    @Override
    public List<SortSpec> defaultSorts() {
        return List.of(
                new SortSpec("operationTime", SortItemDTO.SortDirection.DESC),
                new SortSpec("logId", SortItemDTO.SortDirection.DESC)
        );
    }

    @Override
    public Map<String, QueryFieldSpec<OperationAuditLogEntity>> fields() {
        return Map.ofEntries(
                Map.entry("moduleCode", QueryFieldSpec.text(OperationAuditLogEntity::getModuleCode, "模块编码")),
                Map.entry("moduleName", QueryFieldSpec.text(OperationAuditLogEntity::getModuleName, "模块名称").sortable()),
                Map.entry("description", QueryFieldSpec.text(OperationAuditLogEntity::getDescription, "操作描述")),
                Map.entry("operatorName", QueryFieldSpec.text(OperationAuditLogEntity::getOperatorName, "操作人").sortable()),
                Map.entry("clientIp", QueryFieldSpec.text(OperationAuditLogEntity::getClientIp, "客户端 IP")),
                Map.entry("traceId", QueryFieldSpec.text(OperationAuditLogEntity::getTraceId, "追踪 ID")),
                Map.entry("requestPath", QueryFieldSpec.text(OperationAuditLogEntity::getRequestPath, "请求路径")),
                Map.entry("operationTime", QueryFieldSpec.dateTime(OperationAuditLogEntity::getOperationTime, "操作时间").sortable()),
                Map.entry("action", QueryFieldSpec.enumeration(OperationAuditLogEntity::getAction, "操作类型").sortable().withValueLabels(java.util.Arrays.stream(com.oigit.admin.core.audit.OperationAuditAction.values())
                        .collect(java.util.stream.Collectors.toMap(com.oigit.admin.core.audit.OperationAuditAction::getCode,
                                com.oigit.admin.core.audit.OperationAuditAction::getDesc)))),
                Map.entry("resultStatus", QueryFieldSpec.enumeration(OperationAuditLogEntity::getResultStatus, "操作结果").sortable().withValueLabels(java.util.Arrays.stream(com.oigit.admin.core.audit.OperationAuditResultStatus.values())
                        .collect(java.util.stream.Collectors.toMap(com.oigit.admin.core.audit.OperationAuditResultStatus::getCode,
                                com.oigit.admin.core.audit.OperationAuditResultStatus::getDesc)))),
                Map.entry("logId", QueryFieldSpec.number(OperationAuditLogEntity::getId, "日志 ID").sortable()),
                Map.entry("operatorId", QueryFieldSpec.number(OperationAuditLogEntity::getOperatorId, "操作人 ID")),
                Map.entry("durationMs", QueryFieldSpec.number(OperationAuditLogEntity::getDurationMs, "耗时（毫秒）").sortable()),
                Map.entry("httpStatus", QueryFieldSpec.number(OperationAuditLogEntity::getHttpStatus, "HTTP 状态码")),
                Map.entry("resultCode", QueryFieldSpec.number(OperationAuditLogEntity::getResultCode, "业务状态码"))
        );
    }
}
