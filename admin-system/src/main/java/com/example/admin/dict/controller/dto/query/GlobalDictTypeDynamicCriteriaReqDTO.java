package com.example.admin.dict.controller.dto.query;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.example.admin.core.query.dto.AbstractConditionNodeDTO;
import com.example.admin.core.query.dto.BaseDynamicCriteriaReqDTO;
import com.example.admin.core.query.dto.ConditionGroupDTO;
import com.example.admin.core.query.dto.DateTimeConditionDTO;
import com.example.admin.core.query.dto.SortItemDTO;
import com.example.admin.core.query.dto.TextConditionDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "全局字典类型动态查询条件请求")
public class GlobalDictTypeDynamicCriteriaReqDTO extends BaseDynamicCriteriaReqDTO<
    GlobalDictTypeDynamicCriteriaReqDTO.ConditionNode, GlobalDictTypeDynamicCriteriaReqDTO.SortItem> {

    public enum TextField {
        dictTypeCode,
        dictTypeName
    }

    public enum DateTimeField {
        createTime
    }

    public enum SortField {
        id,
        createTime
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "nodeType")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = GroupCondition.class, name = "compose"),
        @JsonSubTypes.Type(value = TextCondition.class, name = "text"),
        @JsonSubTypes.Type(value = DateTimeCondition.class, name = "dateTime")
    })
    @Schema(
            name = "GlobalDictTypeConditionNode",
            description = "全局字典类型动态查询节点",
            discriminatorProperty = "nodeType",
            oneOf = {
                GroupCondition.class,
                TextCondition.class,
                DateTimeCondition.class
            }
    )
    public interface ConditionNode extends AbstractConditionNodeDTO {
    }

    @Schema(name = "GlobalDictTypeGroupCondition", description = "全局字典类型逻辑分组")
    public static class GroupCondition extends ConditionGroupDTO<ConditionNode> implements ConditionNode {
    }

    @Schema(name = "GlobalDictTypeTextCondition", description = "全局字典类型文本条件")
    public static class TextCondition extends TextConditionDTO<TextField> implements ConditionNode {
    }

    @Schema(name = "GlobalDictTypeDateTimeCondition", description = "全局字典类型时间条件")
    public static class DateTimeCondition extends DateTimeConditionDTO<DateTimeField> implements ConditionNode {
    }

    @Schema(name = "GlobalDictTypeSortItem", description = "全局字典类型排序项")
    public static class SortItem extends SortItemDTO<SortField> {
    }
}
