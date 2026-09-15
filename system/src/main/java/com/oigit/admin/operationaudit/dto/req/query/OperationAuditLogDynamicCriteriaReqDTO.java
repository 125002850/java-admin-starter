package com.oigit.admin.operationaudit.dto.req.query;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.oigit.admin.core.query.dto.AbstractConditionNodeDTO;
import com.oigit.admin.core.query.dto.BaseDynamicCriteriaReqDTO;
import com.oigit.admin.core.query.dto.ConditionGroupDTO;
import com.oigit.admin.core.query.dto.DateTimeConditionDTO;
import com.oigit.admin.core.query.dto.EnumConditionDTO;
import com.oigit.admin.core.query.dto.NumberConditionDTO;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.dto.TextConditionDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "操作日志动态查询条件")
public class OperationAuditLogDynamicCriteriaReqDTO extends BaseDynamicCriteriaReqDTO<
        OperationAuditLogDynamicCriteriaReqDTO.ConditionNode,
        OperationAuditLogDynamicCriteriaReqDTO.SortItem> {

    public enum TextField {
        moduleCode, moduleName, description, operatorName, clientIp, traceId, requestPath
    }

    public enum DateTimeField {
        operationTime
    }

    public enum EnumField {
        action, resultStatus
    }

    public enum NumberField {
        logId, operatorId, durationMs, httpStatus, resultCode
    }

    public enum SortField {
        logId, operationTime, moduleName, action, operatorName, resultStatus, durationMs
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "nodeType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = GroupCondition.class, name = "compose"),
            @JsonSubTypes.Type(value = TextCondition.class, name = "text"),
            @JsonSubTypes.Type(value = DateTimeCondition.class, name = "dateTime"),
            @JsonSubTypes.Type(value = EnumCondition.class, name = "enum"),
            @JsonSubTypes.Type(value = NumberCondition.class, name = "number")
    })
    @Schema(
            name = "OperationAuditLogConditionNode",
            description = "操作日志查询节点",
            discriminatorProperty = "nodeType",
            oneOf = {
                    GroupCondition.class,
                    TextCondition.class,
                    DateTimeCondition.class,
                    EnumCondition.class,
                    NumberCondition.class
            }
    )
    public interface ConditionNode extends AbstractConditionNodeDTO {
    }

    @Schema(name = "OperationAuditLogGroupCondition")
    public static class GroupCondition extends ConditionGroupDTO<ConditionNode> implements ConditionNode {
    }

    @Schema(name = "OperationAuditLogTextCondition")
    public static class TextCondition extends TextConditionDTO<TextField> implements ConditionNode {
    }

    @Schema(name = "OperationAuditLogDateTimeCondition")
    public static class DateTimeCondition extends DateTimeConditionDTO<DateTimeField> implements ConditionNode {
    }

    @Schema(name = "OperationAuditLogEnumCondition")
    public static class EnumCondition extends EnumConditionDTO<EnumField, String> implements ConditionNode {
    }

    @Schema(name = "OperationAuditLogNumberCondition")
    public static class NumberCondition extends NumberConditionDTO<NumberField> implements ConditionNode {
    }

    @Schema(name = "OperationAuditLogSortItem")
    public static class SortItem extends SortItemDTO<SortField> {
    }
}
