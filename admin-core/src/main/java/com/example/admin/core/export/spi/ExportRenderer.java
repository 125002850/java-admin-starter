package com.example.admin.core.export.spi;

import com.example.admin.core.export.model.ExportRenderRequest;
import com.example.admin.core.export.model.RenderedExportFile;

public interface ExportRenderer {

    String fileType();

    RenderedExportFile render(ExportRenderRequest request);
}
