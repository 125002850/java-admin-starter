package com.oigit.admin.core.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import com.oigit.admin.core.query.validation.DynamicQueryLimits;

@Schema(description = "分页请求")
public class PageReqDTO {

    @Schema(description = "页码，从1开始", example = "1")
    @Min(1)
    private long pageNo = 1;

    @Schema(description = "每页条数，最大2000", example = "20", maximum = "2000")
    @Min(1)
    @Max(DynamicQueryLimits.MAX_PAGE_SIZE)
    private long pageSize = 20;

    public long getPageNo() {
        return pageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }
}
