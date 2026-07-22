package com.oigit.admin.dict.controller.dto.query;

import com.oigit.admin.core.query.dto.BasePagedDynamicQueryReqDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

@Schema(description = "全局字典项动态分页查询请求")
public class GlobalDictItemDynamicPageReqDTO extends BasePagedDynamicQueryReqDTO<
    GlobalDictItemDynamicCriteriaReqDTO.ConditionNode,
    GlobalDictItemDynamicCriteriaReqDTO.SortItem> {

    @Override
    @Valid
    @Schema(
        description = "查询条件树",
        discriminatorProperty = "nodeType",
        oneOf = {
            GlobalDictItemDynamicCriteriaReqDTO.GroupCondition.class,
            GlobalDictItemDynamicCriteriaReqDTO.TextCondition.class,
            GlobalDictItemDynamicCriteriaReqDTO.DateTimeCondition.class
        }
    )
    public GlobalDictItemDynamicCriteriaReqDTO.ConditionNode getCondition() {
        return super.getCondition();
    }

    @Override
    @Valid
    @ArraySchema(
        arraySchema = @Schema(description = "排序项"),
        schema = @Schema(implementation = GlobalDictItemDynamicCriteriaReqDTO.SortItem.class)
    )
    public List<GlobalDictItemDynamicCriteriaReqDTO.SortItem> getSort() {
        return super.getSort();
    }
}
