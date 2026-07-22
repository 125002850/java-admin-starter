package com.oigit.admin.dict.controller.dto.query;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.oigit.admin.core.query.dto.AbstractConditionNodeDTO;
import com.oigit.admin.core.query.dto.BaseDynamicCriteriaReqDTO;
import com.oigit.admin.core.query.dto.ConditionGroupDTO;
import com.oigit.admin.core.query.dto.DateTimeConditionDTO;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.dto.TextConditionDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "全局字典项动态查询条件请求")
public class GlobalDictItemDynamicCriteriaReqDTO extends BaseDynamicCriteriaReqDTO<
    GlobalDictItemDynamicCriteriaReqDTO.ConditionNode, GlobalDictItemDynamicCriteriaReqDTO.SortItem> {

    public enum TextField {
        dictTypeCode,
        dictItemCode,
        dictItemName
    }

    public enum DateTimeField {
        createTime
    }

    public enum SortField {
        id,
        sortOrder,
        createTime
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "nodeType")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = GroupCondition.class, name = "compose"),
        @JsonSubTypes.Type(value = TextCondition.class, name = "text"),
        @JsonSubTypes.Type(value = DateTimeCondition.class, name = "dateTime")
    })
    @Schema(
            name = "GlobalDictItemConditionNode",
            description = "全局字典项动态查询节点",
            discriminatorProperty = "nodeType",
            oneOf = {
                GroupCondition.class,
                TextCondition.class,
                DateTimeCondition.class
            }
    )
    public interface ConditionNode extends AbstractConditionNodeDTO {
    }

    @Schema(name = "GlobalDictItemGroupCondition", description = "全局字典项逻辑分组")
    public static class GroupCondition extends ConditionGroupDTO<ConditionNode> implements ConditionNode {
    }

    @Schema(name = "GlobalDictItemTextCondition", description = "全局字典项文本条件")
    public static class TextCondition extends TextConditionDTO<TextField> implements ConditionNode {
    }

    @Schema(name = "GlobalDictItemDateTimeCondition", description = "全局字典项时间条件")
    public static class DateTimeCondition extends DateTimeConditionDTO<DateTimeField> implements ConditionNode {
    }

    @Schema(name = "GlobalDictItemSortItem", description = "全局字典项排序项")
    public static class SortItem extends SortItemDTO<SortField> {
    }
}
