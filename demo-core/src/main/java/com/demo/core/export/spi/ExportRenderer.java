package com.demo.core.export.spi;

import com.demo.core.export.model.ExportRenderRequest;
import com.demo.core.export.model.RenderedExportFile;

public interface ExportRenderer {

    String fileType();

    RenderedExportFile render(ExportRenderRequest request);
}
