package com.oigit.admin.schedule.dto.req.query;

import com.oigit.admin.core.query.dto.BasePagedDynamicQueryReqDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

@Schema(description = "定时任务分页查询请求")
public class ScheduleJobPageQueryReqDTO extends BasePagedDynamicQueryReqDTO<
    ScheduleJobDynamicCriteriaReqDTO.ConditionNode,
    ScheduleJobDynamicCriteriaReqDTO.SortItem> {

    @Schema(description = "关键字，模糊匹配任务名称和任务编码")
    private String keyword;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    @Override @Valid
    @Schema(description = "查询条件树",
        implementation = ScheduleJobDynamicCriteriaReqDTO.ConditionNode.class)
    public ScheduleJobDynamicCriteriaReqDTO.ConditionNode getCondition() {
        return super.getCondition();
    }

    @Override @Valid
    @ArraySchema(arraySchema = @Schema(description = "排序项"),
        schema = @Schema(implementation = ScheduleJobDynamicCriteriaReqDTO.SortItem.class))
    public List<ScheduleJobDynamicCriteriaReqDTO.SortItem> getSort() {
        return super.getSort();
    }
}
