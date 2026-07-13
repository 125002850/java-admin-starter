package com.example.admin.core.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;

@Schema(description = "分页结果")
public class PageResult<T> {

    @NotNull
    @Schema(description = "总条数", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private long total;

    @NotNull
    @Schema(description = "当前页数据列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> list;

    public PageResult(List<T> list, long total) {
        this.list = Objects.requireNonNull(list, "list must not be null");
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = Objects.requireNonNull(list, "list must not be null");
    }
}
