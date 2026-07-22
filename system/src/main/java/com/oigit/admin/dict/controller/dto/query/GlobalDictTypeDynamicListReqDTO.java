package com.oigit.admin.dict.controller.dto.query;

import com.oigit.admin.core.query.dto.BasePagedDynamicQueryReqDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

@Schema(description = "全局字典类型动态分页查询请求")
public class GlobalDictTypeDynamicListReqDTO extends BasePagedDynamicQueryReqDTO<
    GlobalDictTypeDynamicCriteriaReqDTO.ConditionNode,
    GlobalDictTypeDynamicCriteriaReqDTO.SortItem> {

    @Schema(description = "关键字，模糊匹配字典类型编码和名称")
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    @Override
    @Valid
    @Schema(
        description = "查询条件树",
        implementation = GlobalDictTypeDynamicCriteriaReqDTO.ConditionNode.class
    )
    public GlobalDictTypeDynamicCriteriaReqDTO.ConditionNode getCondition() {
        return super.getCondition();
    }

    @Override
    @Valid
    @ArraySchema(
        arraySchema = @Schema(description = "排序项"),
        schema = @Schema(implementation = GlobalDictTypeDynamicCriteriaReqDTO.SortItem.class)
    )
    public List<GlobalDictTypeDynamicCriteriaReqDTO.SortItem> getSort() {
        return super.getSort();
    }
}
