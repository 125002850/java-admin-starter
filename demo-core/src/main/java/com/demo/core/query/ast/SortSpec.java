package com.demo.core.query.ast;

import com.demo.core.query.dto.SortItemDTO;

public class SortSpec {

    private String fieldKey;
    private SortItemDTO.SortDirection direction;

    public SortSpec() {
    }

    public SortSpec(String fieldKey, SortItemDTO.SortDirection direction) {
        this.fieldKey = fieldKey;
        this.direction = direction;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public SortItemDTO.SortDirection getDirection() {
        return direction;
    }

    public void setDirection(SortItemDTO.SortDirection direction) {
        this.direction = direction;
    }
}
