package com.oigit.admin.core.export.spi;

import com.oigit.admin.core.export.model.ExportRenderRequest;
import com.oigit.admin.core.export.model.RenderedExportFile;

public interface ExportRenderer {

    String fileType();

    RenderedExportFile render(ExportRenderRequest request);
}
