package com.oigit.admin.core.export.model;

public class ExportColumn {

    private String field;
    private String title;
    private Integer order;

    public ExportColumn() {
    }

    public ExportColumn(String field, String title, Integer order) {
        this.field = field;
        this.title = title;
        this.order = order;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
