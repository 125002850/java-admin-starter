package com.oigit.admin.core.export.model;

import java.util.Collections;
import java.util.List;

public class ExportRenderRequest {

    private String fileName;
    private List<ExportColumn> columns = Collections.emptyList();
    private List<?> rows = Collections.emptyList();
    private Iterable<?> rowSource;
    private List<ExportSheet> sheets = Collections.emptyList();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<ExportColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<ExportColumn> columns) {
        this.columns = columns;
    }

    public List<?> getRows() {
        return rows;
    }

    public void setRows(List<?> rows) {
        this.rows = rows;
    }
    public List<ExportSheet> getSheets() { return sheets; }
    public void setSheets(List<ExportSheet> sheets) { this.sheets = sheets; }
    public Iterable<?> getRowSource() { return rowSource == null ? rows : rowSource; }
    public void setRowSource(Iterable<?> rowSource) { this.rowSource = rowSource; }
}
