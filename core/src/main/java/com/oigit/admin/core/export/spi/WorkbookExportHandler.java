package com.oigit.admin.core.export.spi;

import com.oigit.admin.core.export.model.ExportColumn;
import com.oigit.admin.core.export.model.ExportSheet;
import com.oigit.admin.core.export.model.RenderedExportFile;
import java.util.List;
import java.io.IOException;

/** 多工作表导出仍由统一执行服务负责文件保存和记录生命周期。 */
public interface WorkbookExportHandler<Q> extends ExportHandler<Q> {

    List<ExportSheet> querySheets(Q query);

    /** 允许处理器在同一只读快照内完成分批查询和本地渲染；文件上传由执行服务在其后完成。 */
    default RenderedExportFile renderWorkbook(Q query, WorkbookRenderer render) throws IOException {
        return render.apply(querySheets(query));
    }

    @FunctionalInterface
    interface WorkbookRenderer {
        RenderedExportFile apply(List<ExportSheet> sheets) throws IOException;
    }

    @Override
    default List<ExportColumn> columns(Q query) {
        return List.of();
    }

    @Override
    default List<?> queryRows(Q query) {
        return List.of();
    }
}
