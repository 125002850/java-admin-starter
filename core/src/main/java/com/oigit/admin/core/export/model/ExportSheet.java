package com.oigit.admin.core.export.model;

import java.util.List;

/** 工作表数据按需遍历，支持列表及分批行源；数值对象可保留为数值单元格。 */
public record ExportSheet(String name, List<ExportColumn> columns, Iterable<?> rows) {
}
