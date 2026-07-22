package com.oigit.admin.core.query.ast;

import java.util.List;

public class QueryAst {

    private ConditionAstNode root;
    private List<SortSpec> sorts;
    private Long pageNo;
    private Long pageSize;

    public ConditionAstNode getRoot() {
        return root;
    }

    public void setRoot(ConditionAstNode root) {
        this.root = root;
    }

    public List<SortSpec> getSorts() {
        return sorts;
    }

    public void setSorts(List<SortSpec> sorts) {
        this.sorts = sorts;
    }

    public Long getPageNo() {
        return pageNo;
    }

    public void setPageNo(Long pageNo) {
        this.pageNo = pageNo;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }
}
