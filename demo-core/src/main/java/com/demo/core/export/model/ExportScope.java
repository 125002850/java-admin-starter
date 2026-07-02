package com.demo.core.export.model;

public class ExportScope {

    public static final String DYNAMIC_QUERY_SUMMARY = "动态查询条件";

    private final String fileNamePart;
    private final String summary;

    private ExportScope(String fileNamePart, String summary) {
        this.fileNamePart = hasText(fileNamePart) ? fileNamePart.trim() : "导出";
        this.summary = hasText(summary) ? summary.trim() : this.fileNamePart;
    }

    public static ExportScope of(String fileNamePart, String summary) {
        return new ExportScope(fileNamePart, summary);
    }

    public static ExportScope allData() {
        return of("全部", "全部数据");
    }

    public static ExportScope dynamicQuery() {
        return of("筛选", DYNAMIC_QUERY_SUMMARY);
    }

    public static ExportScope selectedCount(int count) {
        return of("选中" + count + "条", "按ID导出 " + count + " 条");
    }

    public String getFileNamePart() {
        return fileNamePart;
    }

    public String getSummary() {
        return summary;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
