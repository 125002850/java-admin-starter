package com.oigit.admin.core.export.spi;

import java.util.List;

/**
 * 可按固定页大小读取的导出场景，用于流式渲染和有界内存打包。
 */
public interface ChunkedExportHandler<Q> extends PackageableExportHandler<Q> {

    List<?> queryPage(Q query, long pageNo, long pageSize);
}
