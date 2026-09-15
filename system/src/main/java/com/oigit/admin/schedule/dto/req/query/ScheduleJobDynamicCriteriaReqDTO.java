package com.oigit.admin.schedule.dto.req.query;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.oigit.admin.core.query.dto.AbstractConditionNodeDTO;
import com.oigit.admin.core.query.dto.BaseDynamicCriteriaReqDTO;
import com.oigit.admin.core.query.dto.ConditionGroupDTO;
import com.oigit.admin.core.query.dto.DateTimeConditionDTO;
import com.oigit.admin.core.query.dto.EnumConditionDTO;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.dto.TextConditionDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "定时任务动态查询条件请求")
public class ScheduleJobDynamicCriteriaReqDTO extends BaseDynamicCriteriaReqDTO<
    ScheduleJobDynamicCriteriaReqDTO.ConditionNode,
    ScheduleJobDynamicCriteriaReqDTO.SortItem> {

    public enum TextField { jobName, jobCode, invokeRoute, groupName }
    public enum EnumField { status }
    public enum DateTimeField { createTime, updateTime }
    public enum SortField { id, jobName, jobCode, groupName, cronExpression, invokeRoute, remark, createTime, updateTime }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "nodeType")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = GroupCondition.class, name = "compose"),
        @JsonSubTypes.Type(value = TextCondition.class, name = "text"),
        @JsonSubTypes.Type(value = EnumCondition.class, name = "enum"),
        @JsonSubTypes.Type(value = DateTimeCondition.class, name = "dateTime")
    })
    @Schema(name = "ScheduleJobConditionNode",
        discriminatorProperty = "nodeType",
        oneOf = {GroupCondition.class, TextCondition.class, EnumCondition.class, DateTimeCondition.class})
    public interface ConditionNode extends AbstractConditionNodeDTO {}

    @Schema(name = "ScheduleJobGroupCondition", description = "定时任务逻辑分组")
    public static class GroupCondition extends ConditionGroupDTO<ConditionNode> implements ConditionNode {}

    @Schema(name = "ScheduleJobTextCondition", description = "定时任务文本条件")
    public static class TextCondition extends TextConditionDTO<TextField> implements ConditionNode {}

    @Schema(name = "ScheduleJobEnumCondition", description = "定时任务枚举条件")
    public static class EnumCondition extends EnumConditionDTO<EnumField, String> implements ConditionNode {}

    @Schema(name = "ScheduleJobDateTimeCondition", description = "定时任务时间条件")
    public static class DateTimeCondition extends DateTimeConditionDTO<DateTimeField> implements ConditionNode {}

    @Schema(name = "ScheduleJobSortItem", description = "定时任务排序项")
    public static class SortItem extends SortItemDTO<SortField> {}
}
