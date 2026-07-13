package com.example.admin.mdm.export.controller.dto.query;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.example.admin.core.query.dto.AbstractConditionNodeDTO;
import com.example.admin.core.query.dto.BaseDynamicCriteriaReqDTO;
import com.example.admin.core.query.dto.ConditionGroupDTO;
import com.example.admin.core.query.dto.DateTimeConditionDTO;
import com.example.admin.core.query.dto.EnumConditionDTO;
import com.example.admin.core.query.dto.SortItemDTO;
import com.example.admin.core.query.dto.TextConditionDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "导出记录动态查询条件请求")
public class ExportRecordDynamicCriteriaReqDTO extends BaseDynamicCriteriaReqDTO<
    ExportRecordDynamicCriteriaReqDTO.ConditionNode, ExportRecordDynamicCriteriaReqDTO.SortItem> {

    public enum TextField {
        exportBizCode,
        exportBizName,
        fileName
    }

    public enum DateTimeField {
        createTime,
        finishedTime,
        expireTime
    }

    public enum EnumField {
        status
    }

    public enum SortField {
        createTime,
        finishedTime,
        expireTime,
        downloadCount
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "nodeType")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = GroupCondition.class, name = "compose"),
        @JsonSubTypes.Type(value = TextCondition.class, name = "text"),
        @JsonSubTypes.Type(value = DateTimeCondition.class, name = "dateTime"),
        @JsonSubTypes.Type(value = EnumCondition.class, name = "enum")
    })
    @Schema(
            name = "ExportRecordConditionNode",
            description = "导出记录动态查询节点",
            discriminatorProperty = "nodeType",
            oneOf = {
                GroupCondition.class,
                TextCondition.class,
                DateTimeCondition.class,
                EnumCondition.class
            }
    )
    public interface ConditionNode extends AbstractConditionNodeDTO {
    }

    @Schema(name = "ExportRecordGroupCondition", description = "导出记录逻辑分组")
    public static class GroupCondition extends ConditionGroupDTO<ConditionNode> implements ConditionNode {
    }

    @Schema(name = "ExportRecordTextCondition", description = "导出记录文本条件")
    public static class TextCondition extends TextConditionDTO<TextField> implements ConditionNode {
    }

    @Schema(name = "ExportRecordDateTimeCondition", description = "导出记录时间条件")
    public static class DateTimeCondition extends DateTimeConditionDTO<DateTimeField> implements ConditionNode {
    }

    @Schema(name = "ExportRecordEnumCondition", description = "导出记录枚举条件")
    public static class EnumCondition extends EnumConditionDTO<EnumField, Integer> implements ConditionNode {
    }

    @Schema(name = "ExportRecordSortItem", description = "导出记录排序项")
    public static class SortItem extends SortItemDTO<SortField> {
    }
}
