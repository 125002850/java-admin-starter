package com.demo.core.query.exception;

import com.demo.core.exception.ErrorCode;

public enum DynamicQueryErrorCode implements ErrorCode {

    DYNAMIC_QUERY_UNSUPPORTED_NODE(3000002, "动态查询节点类型不支持"),
    DYNAMIC_QUERY_UNKNOWN_NODE_TYPE(3000003, "动态查询节点类型未知"),
    DYNAMIC_QUERY_INVALID_SORT_FIELD(3000004, "动态查询排序字段非法"),
    DYNAMIC_QUERY_EMPTY_GROUP_CHILDREN(3000005, "动态查询分组子节点不能为空"),
    DYNAMIC_QUERY_INVALID_RANGE(3000006, "动态查询区间参数非法"),
    DYNAMIC_QUERY_INVALID_TEXT_PAYLOAD(3000007, "动态查询文本条件参数非法"),
    DYNAMIC_QUERY_INVALID_ENUM_PAYLOAD(3000008, "动态查询枚举条件参数非法"),
    DYNAMIC_QUERY_IN_VALUE_TOO_LARGE(3000009, "动态查询 IN 集合过大"),
    DYNAMIC_QUERY_PAGE_SIZE_TOO_LARGE(3000010, "动态查询分页大小超限"),
    DYNAMIC_QUERY_TREE_DEPTH_EXCEEDED(3000011, "动态查询条件树深度超限"),
    DYNAMIC_QUERY_NODE_COUNT_EXCEEDED(3000012, "动态查询条件节点数量超限"),
    DYNAMIC_QUERY_COMPLEXITY_EXCEEDED(3000013, "动态查询复杂度超限"),
    DYNAMIC_QUERY_UNSUPPORTED_OPERATOR(3000014, "动态查询操作符不支持");

    private final int code;
    private final String msg;

    DynamicQueryErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
