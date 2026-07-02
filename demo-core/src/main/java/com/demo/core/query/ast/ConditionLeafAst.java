package com.demo.core.query.ast;

public class ConditionLeafAst extends ConditionAstNode {

    private String fieldKey;
    private QueryOperator operator;
    private Object typedValue;

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public QueryOperator getOperator() {
        return operator;
    }

    public void setOperator(QueryOperator operator) {
        this.operator = operator;
    }

    public Object getTypedValue() {
        return typedValue;
    }

    public void setTypedValue(Object typedValue) {
        this.typedValue = typedValue;
    }
}
